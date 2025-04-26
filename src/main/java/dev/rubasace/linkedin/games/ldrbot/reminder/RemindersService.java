package dev.rubasace.linkedin.games.ldrbot.reminder;

import dev.rubasace.linkedin.games.ldrbot.group.GroupInfo;
import dev.rubasace.linkedin.games.ldrbot.user.MissingSessionUserProjection;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUserService;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import dev.rubasace.linkedin.games.ldrbot.util.LinkedinTimeUtils;
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

        GroupInfo groupInfo = new GroupInfo(missingSessionUserProjection.getChatId(), missingSessionUserProjection.getGroupName());
        UserInfo userInfo = new UserInfo(missingSessionUserProjection.getUserId(), missingSessionUserProjection.getUserName(), missingSessionUserProjection.getFirstName(),
                                         missingSessionUserProjection.getLastName());
        applicationEventPublisher.publishEvent(new UserMissingSessionsReminderEvent(this, groupInfo, userInfo));
    }
}
