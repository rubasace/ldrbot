package dev.rubasace.linkedin.games.ldrbot.reminder;

import dev.rubasace.linkedin.games.ldrbot.chat.CustomTelegramClient;
import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.user.MissingSessionUserProjection;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUserService;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import dev.rubasace.linkedin.games.ldrbot.util.BackpressureExecutors;
import dev.rubasace.linkedin.games.ldrbot.util.FormatUtils;
import dev.rubasace.linkedin.games.ldrbot.util.LinkedinTimeUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.concurrent.ExecutorService;

@Transactional(readOnly = true)
@Service
public class RemindersService {

    private static final String USER_MISSING_SESSIONS_REMINDER = """
                ⏰ <b>Don't forget!</b> ⏰
            
                Hey %s! Looks like you're missing some of today’s results.
                Don’t leave your group hanging — submit your screenshots and climb the leaderboard! 💪
            """;

    private static final int MAX_CONCURRENCY = 50;
    public static final int REMINDERS_HOUR = 20;

    private final TelegramUserService telegramUserService;
    private final CustomTelegramClient customTelegramClient;
    private final MissingSessionUserProjectionUserInfoAdapter missingSessionUserProjectionUserInfoAdapter;
    private final MissingSessionUserProjectionChatInfoAdapter missingSessionUserProjectionChatInfoAdapter;
    private final ExecutorService reminderExecutor;

    RemindersService(final TelegramUserService telegramUserService, final CustomTelegramClient customTelegramClient, final MissingSessionUserProjectionUserInfoAdapter missingSessionUserProjectionUserInfoAdapter, final MissingSessionUserProjectionChatInfoAdapter missingSessionUserProjectionChatInfoAdapter) {
        this.telegramUserService = telegramUserService;
        this.customTelegramClient = customTelegramClient;
        this.missingSessionUserProjectionUserInfoAdapter = missingSessionUserProjectionUserInfoAdapter;
        this.missingSessionUserProjectionChatInfoAdapter = missingSessionUserProjectionChatInfoAdapter;
        this.reminderExecutor = BackpressureExecutors.newBackPressureVirtualThreadPerTaskExecutor("reminders", MAX_CONCURRENCY);
    }

    //TODO only remind users at the specified time zone
    @Async
    public void remindMissingUsers() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        telegramUserService.findUsersWithMissingSessions(LinkedinTimeUtils.todayGameDay())
                           .filter(this::shouldRemindNow)
                           .forEach(missingSessionUserProjection -> reminderExecutor.execute(() -> remindMissingUser(missingSessionUserProjection)));
    }

    private boolean shouldRemindNow(MissingSessionUserProjection missingSessionUserProjection) {
        LocalTime groupLocalTime = LocalTime.now(missingSessionUserProjection.getTimeZone());
        return groupLocalTime.getHour() == REMINDERS_HOUR;
    }

    private void remindMissingUser(MissingSessionUserProjection missingSessionUserProjection) {
        ChatInfo chatInfo = missingSessionUserProjectionChatInfoAdapter.adapt(missingSessionUserProjection);
        UserInfo userInfo = missingSessionUserProjectionUserInfoAdapter.adapt(missingSessionUserProjection);
        customTelegramClient.message(USER_MISSING_SESSIONS_REMINDER.formatted(FormatUtils.formatUserMention(userInfo)), chatInfo.chatId());
    }


}
