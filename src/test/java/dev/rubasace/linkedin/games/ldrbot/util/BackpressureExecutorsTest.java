package dev.rubasace.linkedin.games.ldrbot.util;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;

class BackpressureExecutorsTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackpressureExecutorsTest.class);

    ExecutorService executorService = BackpressureExecutors.newBackPressureVirtualThreadPerTaskExecutor("test", 20);


    @Test
    void name() {
        IntStream.range(0, 100)
                 .forEach(e -> executorService.execute(() -> doSomething(e)));
    }

    private void doSomething(int i) {
        try {
            LOGGER.info("[{}] - {}", Thread.currentThread().getName(), i);
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}