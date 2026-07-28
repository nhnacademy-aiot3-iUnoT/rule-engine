package com.nhnacademy.ruleengine.engine.core;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// 비동기 Flow의 최종 처리 결과를 최초 요청자에게 전달한다.
public class FlowProcessingCompletion {

    private final CompletableFuture<Void> future = new CompletableFuture<>();

    public void complete() {
        future.complete(null);
    }

    public void completeExceptionally(Throwable throwable) {
        future.completeExceptionally(throwable);
    }

    public void await(Duration timeout)
            throws InterruptedException, ExecutionException, TimeoutException {
        future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }
}
