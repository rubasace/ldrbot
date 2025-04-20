package dev.rubasace.linkedin.games_tracker.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@EnableAsync
@Configuration
public class AsyncConfiguration {

    public static final String BACKGROUND_TASKS_EXECUTOR_NAME = "backgroundTasksExecutor";
    public static final String NOTIFICATION_LISTENER_EXECUTOR_NAME = "notificationListenerExecutor";

    @Bean(BACKGROUND_TASKS_EXECUTOR_NAME)
    public Executor eventListenerExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean(NOTIFICATION_LISTENER_EXECUTOR_NAME)
    public Executor notificationListenerExecutor() {
        return Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());
    }
}