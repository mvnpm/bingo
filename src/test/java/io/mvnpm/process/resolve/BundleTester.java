package io.mvnpm.process.resolve;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public abstract class BundleTester {

    public static void cleanUp(String version) throws IOException {
        deleteRecursive(Resolvers.getLocation(version));
    }

    public static void deleteRecursive(Path source) throws IOException {
        if (!Files.exists(source)) {
            return;
        }
        try (final Stream<Path> paths = Files.walk(source)) {
            paths.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);

        }
    }
}
