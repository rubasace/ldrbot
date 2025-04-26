package dev.rubasace.linkedin.games.ldrbot.reminder;

import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
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
    private final MissingSessionUserProjectionUserInfoAdapter missingSessionUserProjectionUserInfoAdapter;
    private final MissingSessionUserProjectionChatInfoAdapter missingSessionUserProjectionChatInfoAdapter;

    public RemindersService(final TelegramUserService telegramUserService, final ApplicationEventPublisher applicationEventPublisher, final MissingSessionUserProjectionUserInfoAdapter missingSessionUserProjectionUserInfoAdapter, final MissingSessionUserProjectionChatInfoAdapter missingSessionUserProjectionChatInfoAdapter) {
        this.telegramUserService = telegramUserService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.missingSessionUserProjectionUserInfoAdapter = missingSessionUserProjectionUserInfoAdapter;
        this.missingSessionUserProjectionChatInfoAdapter = missingSessionUserProjectionChatInfoAdapter;
    }

    //TODO also parallelize and run in isolated transactions
    @Transactional
    public void remindMissingUsers() {
        telegramUserService.findUsersWithMissingSessions(LinkedinTimeUtils.todayGameDay())
                           .forEach(this::remindMissingUser);
    }

    private void remindMissingUser(MissingSessionUserProjection missingSessionUserProjection) {

        ChatInfo chatInfo = missingSessionUserProjectionChatInfoAdapter.adapt(missingSessionUserProjection);
        UserInfo userInfo = missingSessionUserProjectionUserInfoAdapter.adapt(missingSessionUserProjection);
        applicationEventPublisher.publishEvent(new UserMissingSessionsReminderEvent(this, chatInfo, userInfo));
    }


}
