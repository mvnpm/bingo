package io.mvnpm.process.model;

import java.io.Closeable;

public record WatchStartResult(WatchExecuteResult firstBuildResult, WatchProcess process) {

    public interface WatchProcess extends Closeable {

        boolean isAlive();

    }
}
