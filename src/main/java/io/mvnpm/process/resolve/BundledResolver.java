package io.mvnpm.process.resolve;

import io.mvnpm.process.FilenameMapper;

import static io.mvnpm.process.resolve.Resolvers.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public class BundledResolver implements Resolver {
    private static final Logger logger = Logger.getLogger(DownloadResolver.class.getName());
    private final Resolver fallbackResolver;
    private final FilenameMapper filenameMapper;

    public BundledResolver(Resolver fallbackResolver, FilenameMapper filenameMapper) {
        this.fallbackResolver = fallbackResolver;
        this.filenameMapper = filenameMapper;
    }

    @Override
    public Path resolve(String version) throws IOException {
        final Path path = getLocation(version);
        final Path executablePath = path.resolve(filenameMapper.executable());
        if (Files.isExecutable(executablePath)) {
            return executablePath;
        }

        final String tgz = filenameMapper.tarFileName(version, CLASSIFIER);
        final InputStream resource = getClass().getResourceAsStream(tgz);

        if (resource != null) {
            final Path bundleDir = extract(resource, version);
            return requireExecutablePath(bundleDir);
        }

        return fallbackResolver.resolve(version);

    }

}
