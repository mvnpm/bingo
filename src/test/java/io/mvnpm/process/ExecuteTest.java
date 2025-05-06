package io.mvnpm.process;

import io.mvnpm.process.resolve.Resolver;
import org.junit.jupiter.api.Test;
import io.mvnpm.process.model.ExecuteResult;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class ExecuteTest {

    @Test
    public void shouldExecuteEsBuild() throws Exception {
        String defaultVersion = "0.23.0";
        final Path path = Resolver.create(new EsBuildFilenameMapper()).resolve(defaultVersion);
        String workingDirectory = System.getProperty("user.dir");
        final ExecuteResult executeResult = new Execute(Paths.get(workingDirectory), path.toFile(), new EsBuildParameters())
                .executeAndWait();
        assertEquals(defaultVersion + "\n", executeResult.output());
    }

    public static class EsBuildParameters implements ProcessParameters {

        @Override
        public String[] toArguments() {
            return new String[]{"--version"};
        }
    }

}