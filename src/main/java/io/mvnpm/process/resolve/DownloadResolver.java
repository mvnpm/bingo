package io.mvnpm.process.resolve;

import io.mvnpm.process.FilenameMapper;

import static io.mvnpm.process.resolve.Resolvers.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public class DownloadResolver implements Resolver {

    private static final Logger logger = Logger.getLogger(DownloadResolver.class.getName());

//    public static final String ESBUILD_URL_TEMPLATE = "https://registry.npmjs.org/@esbuild/%1$s/-";
//    public static final String MVNPM_URL_TEMPLATE = "https://github.com/mvnpm/esbuild/releases/download/v%1$s";
//    private static final String FILE_NAME = "esbuild.tgz";
    private final FilenameMapper filenameMapper;

    public DownloadResolver(FilenameMapper filenameMapper) {
        this.filenameMapper = filenameMapper;
    }

    @Override
    public Path resolve(String version) throws IOException {
        final String url = filenameMapper.downloadUrl(version, CLASSIFIER);

        final Path destination = createDestination(version);
        final Path tarFile = destination.resolve(filenameMapper.tarFileName(CLASSIFIER, version));

        try {
            downloadFile(new URL(url), tarFile);
        } catch (IOException e) {
            throw new UncheckedIOException("could not download: " + url, e);
        }
        try {
            final File file = destination.toFile();
            logger.fine("Extracting file from " + tarFile + " to " + file);
            final Path extracted = extract(Files.newInputStream(tarFile), file);
            return requireExecutablePath(extracted.resolve(filenameMapper.executable()));
        } catch (IOException e) {
            Files.deleteIfExists(tarFile);
            throw new UncheckedIOException("could not resolve executable " + destination, e);
        }
    }

    void downloadFile(URL url, Path destination) throws IOException {
        if (Files.exists(destination)) {
            logger.fine("Tar file is already downloaded");
            return;
        }
        ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
        try (FileOutputStream fileOutputStream = new FileOutputStream(destination.toFile())) {
            logger.fine("Downloading file from " + url + " to " + destination);
            fileOutputStream.getChannel()
                    .transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        }
    }

//    private String getDownloadUrl(String version) {
//        final String tgz = getTgzPath(version);
//        if (version.contains("mvnpm")) {
//            return MVNPM_URL_TEMPLATE.formatted(version.substring(version.lastIndexOf("-") + 1)) + tgz;
//        }
//        return ESBUILD_URL_TEMPLATE.formatted(CLASSIFIER) + tgz;
//    }

}
