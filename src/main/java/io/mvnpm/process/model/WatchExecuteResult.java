package io.mvnpm.process.model;

import io.mvnpm.process.ProcessException;

public record WatchExecuteResult(String output, ProcessException processException) {
    public WatchExecuteResult(String output) {
        this(output, null);
    }

    public boolean isSuccess() {
        return processException == null;
    }
}
