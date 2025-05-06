package io.mvnpm.process.resolve;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.mvnpm.process.EsBuildFilenameMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DownloadResolverTest extends BundleTester {
    private static final String TEST_VERSION = "0.19.9";

    @AfterAll
    public static void cleanUp() throws IOException {
        cleanUp(TEST_VERSION);
    }

    @Test
    public void download() throws IOException {
        // when
        final Path path = new DownloadResolver(new EsBuildFilenameMapper()).resolve(TEST_VERSION);

        // then
        assertTrue(Files.exists(path), path + " does not exist");
    }
}
