package dev.rubasace.linkedin.games.ldrbot.util;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class BackpressureExecutors {

    public static ExecutorService newBackPressureVirtualThreadPerTaskExecutor(String name, int maxConcurrency) {
        ThreadFactory factory = Thread.ofVirtual().name(name, 1).factory();
        ExecutorService delegate = Executors.newFixedThreadPool(maxConcurrency, factory);
        return new BackpressureExecutorService(delegate, maxConcurrency, name);
    }

    private static class BackpressureExecutorService extends AbstractExecutorService {

        private final ExecutorService delegate;
        private final Semaphore semaphore;
        private final String name;

        public BackpressureExecutorService(ExecutorService delegate, int maxConcurrency, String name) {
            this.delegate = delegate;
            this.semaphore = new Semaphore(maxConcurrency);
            this.name = name;
        }

        @Override
        public void execute(@NotNull Runnable command) {
            try {
                semaphore.acquire();
                delegate.execute(() -> {
                    try {
                        command.run();
                    } finally {
                        semaphore.release();
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RejectedExecutionException(name + ": Interrupted while acquiring permit", e);
            }
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        @NotNull
        @Override
        public List<Runnable> shutdownNow() {
            return delegate.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }
    }
}
