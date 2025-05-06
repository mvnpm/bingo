package io.mvnpm.process;

import io.mvnpm.process.model.WatchExecuteResult;

public interface ProcessEventListener {

    void onExecute(WatchExecuteResult result);

}
