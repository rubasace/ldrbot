package dev.rubasace.linkedin.games.ldrbot.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@EnableAsync
@Configuration
public class ExecutorsConfiguration {

    public static final String BACKGROUND_TASKS_EXECUTOR_NAME = "backgroundTasksExecutor";
    public static final String NOTIFICATION_LISTENER_EXECUTOR_NAME = "notificationListenerExecutor";
    public static final String SCHEDULED_TASKS_EXECUTOR_NAME = "scheduledTasksExecutor";

    @Bean(BACKGROUND_TASKS_EXECUTOR_NAME)
    public Executor backgroundTasksExecutor() {
        return Executors.newFixedThreadPool(5, Thread.ofVirtual().name("background-tasks", 1).factory());
    }

    @Bean(NOTIFICATION_LISTENER_EXECUTOR_NAME)
    public Executor notificationListenerExecutor() {
        return Executors.newSingleThreadExecutor(Thread.ofVirtual().name("notifications").factory());
    }

    @Bean(ExecutorsConfiguration.SCHEDULED_TASKS_EXECUTOR_NAME)
    public ScheduledExecutorService scheduledTasksExecutor() {
        return Executors.newScheduledThreadPool(4, Thread.ofVirtual().name("scheduled", 1).factory());
    }

}