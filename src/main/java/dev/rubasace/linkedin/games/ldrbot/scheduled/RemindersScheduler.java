package dev.rubasace.linkedin.games.ldrbot.scheduled;

import dev.rubasace.linkedin.games.ldrbot.configuration.ExecutorsConfiguration;
import dev.rubasace.linkedin.games.ldrbot.reminder.RemindersService;
import dev.rubasace.linkedin.games.ldrbot.util.LinkedinTimeUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class RemindersScheduler implements ApplicationListener<ApplicationReadyEvent> {

    private final RemindersService remindersService;

    RemindersScheduler(final RemindersService remindersService) {
        this.remindersService = remindersService;
    }

    @Scheduled(cron = "0 0 * * * *", zone = LinkedinTimeUtils.LINKEDIN_ZONE, scheduler = ExecutorsConfiguration.SCHEDULED_TASKS_EXECUTOR_NAME)
    public void remindMissingUsers() {
        remindersService.remindMissingUsers();
    }

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        this.remindersService.remindMissingUsers();
    }
}
