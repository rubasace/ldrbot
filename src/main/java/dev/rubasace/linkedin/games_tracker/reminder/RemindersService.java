package dev.rubasace.linkedin.games_tracker.reminder;

import dev.rubasace.linkedin.games_tracker.user.MissingSessionUserProjection;
import dev.rubasace.linkedin.games_tracker.user.TelegramUserService;
import dev.rubasace.linkedin.games_tracker.util.LinkedinTimeUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RemindersService {

    private final TelegramUserService telegramUserService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public RemindersService(final TelegramUserService telegramUserService, final ApplicationEventPublisher applicationEventPublisher) {
        this.telegramUserService = telegramUserService;
        this.applicationEventPublisher = applicationEventPublisher;
    }


    //TODO also parallelize and run in isolated transactions
    @Transactional
    public void remindMissingUsers() {
        telegramUserService.findUsersWithMissingSessions(LinkedinTimeUtils.todayGameDay())
                           .forEach(this::remindMissingUser);
    }

    private void remindMissingUser(MissingSessionUserProjection missingSessionUserProjection) {
        applicationEventPublisher.publishEvent(new UserMissingSessionsReminderEvent(this, missingSessionUserProjection.getUserName(), missingSessionUserProjection.getChatId()));
    }
}
