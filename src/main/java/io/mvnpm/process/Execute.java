package io.mvnpm.process;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.mvnpm.process.model.ExecuteResult;
import io.mvnpm.process.model.WatchExecuteResult;
import io.mvnpm.process.model.WatchStartResult;

public class Execute {

    private static final ExecutorService EXECUTOR_STREAMER = Executors.newSingleThreadExecutor(r -> {
        final Thread t = new Thread(r, "Process stdout streamer");
        t.setDaemon(true);
        return t;
    });
    private static final Logger logger = Logger.getLogger(Execute.class.getName());

    private final Path workDir;
    private final File executable;
    private ProcessParameters processParameters;
    private String[] args;

    public Execute(Path workDir, File executable, ProcessParameters processParameters) {
        this.workDir = workDir;
        this.executable = executable;
        this.processParameters = processParameters;
    }

    public Execute(Path workDir, File executable, String[] args) {
        this.workDir = workDir;
        this.executable = executable;
        this.args = args;
    }

    public ExecuteResult executeAndWait() throws IOException {
        final Process process = createProcess(getCommand(), Optional.empty());
        try {
            final int exitCode = process.waitFor();
            final String content = readStream(process.getInputStream());
            final String errors = readStream(process.getErrorStream());
            if (exitCode != 0) {
                throw new ProcessException(errors.isEmpty() ? "Unexpected Error during bundling" : errors, content);
            }
            return new ExecuteResult(content);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    public WatchStartResult watch(ProcessEventListener listener) throws IOException {
        final Process process = createProcess(getCommand(), Optional.of(listener));
        final ExecutorService executorStreamer = Executors
                .newSingleThreadExecutor(r -> new Thread(r, "Process watch stdout streamer"));
        final ExecutorService executor = Executors
                .newSingleThreadExecutor(r -> new Thread(r, "Process listeners notify"));
        final AtomicReference<WatchExecuteResult> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        final WatchStartResult.WatchProcess watchProcess = new WatchStartResult.WatchProcess() {
            @Override
            public boolean isAlive() {
                return process.isAlive();
            }

            @Override
            public void close() throws IOException {
                process.destroyForcibly();
                executorStreamer.shutdownNow();
                executor.shutdownNow();
                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
                if (latch.getCount() == 1) {
                    latch.countDown();
                }
            }
        };
        try {
            final InputStream processStream = process.getInputStream();

            executorStreamer.execute(new Streamer(executor, process::isAlive, processStream, (r) -> {
                if (latch.getCount() == 1) {
                    result.set(r);
                    latch.countDown();
                } else {
                    listener.onExecute(r);
                }
            }, r -> {
                if (latch.getCount() == 1) {
                    result.set(r);
                    latch.countDown();
                } else if (!r.isSuccess()) {
                    listener.onExecute(r);
                }
            }));
            latch.await();
            if (!process.isAlive() && !result.get().isSuccess()) {
                throw result.get().processException();
            }

            return new WatchStartResult(result.get(), watchProcess);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            watchProcess.close();
            throw new RuntimeException(e);
        }
    }

    private String[] getCommand() {
        String[] command = args != null ? getCommand(args) : getCommand(processParameters);
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "running process with flags: \n > `{0}`", String.join(" ", command));
        }
        return command;
    }

    private String[] getCommand(ProcessParameters processParameters) {
        String[] params = processParameters.toArguments();
        return getCommand(params);
    }

    private String[] getCommand(String[] args) {
        List<String> argList = new ArrayList<>(args.length + 1);
        argList.add(executable.toString());
        argList.addAll(Arrays.asList(args));

        return argList.toArray(String[]::new);
    }

    public Process createProcess(final String[] command, final Optional<ProcessEventListener> listener) throws IOException {
        return new ProcessBuilder().redirectErrorStream(listener.isPresent()).directory(workDir.toFile())
                .command(command).start();
    }

    private record Streamer(ExecutorService executorBuild, BooleanSupplier isAlive, InputStream processStream,
                            ProcessEventListener listener, Consumer<WatchExecuteResult> onExit) implements Runnable {

        @Override
        public void run() {
            final AtomicBoolean hasError = new AtomicBoolean();
            final StringBuilder outputBuilder = new StringBuilder();
            consumeStream(isAlive, processStream, l -> {
                logger.fine(l);
                outputBuilder.append("\n").append(l);
                if (l.contains("build finished")) {
                    logger.fine("Build finished!");
                    final String output = outputBuilder.toString();
                    final boolean error = hasError.getAndSet(false);
                    outputBuilder.setLength(0);
                    executorBuild.execute(() -> {
                        if (!error) {
                            listener.onExecute(new WatchExecuteResult(output));
                        } else {
                            listener.onExecute(
                                    new WatchExecuteResult(output, new ProcessException("Error during bundling", output)));
                        }
                    });
                } else if (l.contains("[ERROR]")) {
                    hasError.set(true);
                }
            });
            if (!hasError.get()) {
                onExit.accept(new WatchExecuteResult(outputBuilder.toString()));
            } else {
                onExit.accept(new WatchExecuteResult(outputBuilder.toString(),
                        new ProcessException("Process exited with error", outputBuilder.toString())));
            }
        }
    }

    private static String readStream(InputStream stream) {
        final StringBuilder s = new StringBuilder();
        consumeStream(() -> true, stream, l -> s.append(l).append("\n"));
        return s.toString();
    }

    private static void consumeStream(BooleanSupplier stayAlive, InputStream stream, Consumer<String> newLineConsumer) {
        try (
                final InputStreamReader in = new InputStreamReader(stream, StandardCharsets.UTF_8);
                final BufferedReader reader = new BufferedReader(in)) {
            String line;
            while ((line = reader.readLine()) != null) {
                newLineConsumer.accept(line);
                if (!stayAlive.getAsBoolean()) {
                    break;
                }
            }
        } catch (IOException e) {
            // ignore
        }
    }

}
