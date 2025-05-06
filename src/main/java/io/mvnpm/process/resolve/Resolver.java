package io.mvnpm.process.resolve;

import io.mvnpm.process.FilenameMapper;

import java.io.IOException;
import java.nio.file.Path;

public interface Resolver {
    Path resolve(String version) throws IOException;

    static Resolver create(FilenameMapper filenameMapper) {
        final DownloadResolver downloadResolver = new DownloadResolver(filenameMapper);
        return new BundledResolver(downloadResolver, filenameMapper);
    }
}
