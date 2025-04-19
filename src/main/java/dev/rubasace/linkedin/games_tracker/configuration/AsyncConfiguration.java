package dev.rubasace.linkedin.games_tracker.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@EnableAsync
@Configuration
public class AsyncConfiguration {

    public static final String EVENT_LISTENER_EXECUTOR_NAME = "eventListenerExecutor";

    @Bean(EVENT_LISTENER_EXECUTOR_NAME)
    public Executor eventListenerExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}