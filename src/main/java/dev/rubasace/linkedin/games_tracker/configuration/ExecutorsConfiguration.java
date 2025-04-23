package dev.rubasace.linkedin.games_tracker.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@EnableAsync
@Configuration
public class ExecutorsConfiguration {

    public static final String BACKGROUND_TASKS_EXECUTOR_NAME = "backgroundTasksExecutor";
    public static final String NOTIFICATION_LISTENER_EXECUTOR_NAME = "notificationListenerExecutor";

    @Bean(BACKGROUND_TASKS_EXECUTOR_NAME)
    public Executor backgroundTasksExecutor() {
        return Executors.newFixedThreadPool(5, Thread.ofVirtual().name("background-tasks", 1).factory());
    }

    @Bean(NOTIFICATION_LISTENER_EXECUTOR_NAME)
    public Executor notificationListenerExecutor() {
        return Executors.newSingleThreadExecutor(Thread.ofVirtual().name("notifications").factory());
    }
}