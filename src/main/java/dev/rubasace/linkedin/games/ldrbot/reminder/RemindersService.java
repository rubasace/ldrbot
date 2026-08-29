package dev.rubasace.linkedin.games.ldrbot.reminder;

import dev.rubasace.linkedin.games.ldrbot.chat.CustomTelegramClient;
import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.metrics.MetricsConstants;
import dev.rubasace.linkedin.games.ldrbot.user.MissingSessionUserProjection;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUserService;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import dev.rubasace.linkedin.games.ldrbot.util.BackpressureExecutors;
import dev.rubasace.linkedin.games.ldrbot.util.FormatUtils;
import dev.rubasace.linkedin.games.ldrbot.util.LinkedinTimeUtils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

@Transactional(readOnly = true)
@Service
public class RemindersService {

    private static final String USER_MISSING_SESSIONS_REMINDER = """
                ⏰ <b>Don't forget!</b> ⏰
            
                Hey %s! Looks like you're missing some of today's results.
                Don't leave your group hanging — submit your screenshots and climb the leaderboard! 💪
            """;

    private static final int MAX_CONCURRENCY = 50;
    public static final int REMINDERS_HOUR = 20;
    private static final String TASK_NAME = "reminder";

    private final TelegramUserService telegramUserService;
    private final CustomTelegramClient customTelegramClient;
    private final MissingSessionUserProjectionUserInfoAdapter missingSessionUserProjectionUserInfoAdapter;
    private final MissingSessionUserProjectionChatInfoAdapter missingSessionUserProjectionChatInfoAdapter;
    private final ExecutorService reminderExecutor;
    private final MeterRegistry meterRegistry;
    private final Timer backgroundDurationTimer;
    private final Counter backgroundErrorsCounter;

    RemindersService(final TelegramUserService telegramUserService,
                     final CustomTelegramClient customTelegramClient,
                     final MissingSessionUserProjectionUserInfoAdapter missingSessionUserProjectionUserInfoAdapter,
                     final MissingSessionUserProjectionChatInfoAdapter missingSessionUserProjectionChatInfoAdapter,
                     final MeterRegistry meterRegistry) {
        this.telegramUserService = telegramUserService;
        this.customTelegramClient = customTelegramClient;
        this.missingSessionUserProjectionUserInfoAdapter = missingSessionUserProjectionUserInfoAdapter;
        this.missingSessionUserProjectionChatInfoAdapter = missingSessionUserProjectionChatInfoAdapter;
        this.reminderExecutor = BackpressureExecutors.newBackPressureVirtualThreadPerTaskExecutor("reminders", MAX_CONCURRENCY);
        this.meterRegistry = meterRegistry;
        this.backgroundDurationTimer = meterRegistry.timer(MetricsConstants.BACKGROUND_DURATION,
                MetricsConstants.TAG_TASK_NAME, TASK_NAME);
        this.backgroundErrorsCounter = meterRegistry.counter(MetricsConstants.BACKGROUND_ERRORS,
                MetricsConstants.TAG_TASK_NAME, TASK_NAME);
    }

    public void remindMissingUsers() {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            telegramUserService.findUsersWithMissingSessions(LinkedinTimeUtils.todayGameDay())
                               .filter(this::shouldRemindNow)
                               .forEach(missingSessionUserProjection -> reminderExecutor.execute(() -> remindMissingUser(missingSessionUserProjection)));
        } finally {
            sample.stop(backgroundDurationTimer);
        }
    }

    private boolean shouldRemindNow(MissingSessionUserProjection missingSessionUserProjection) {
        ZoneId groupZoneId = Optional.ofNullable(missingSessionUserProjection.getTimeZone())
                                     .orElse(LinkedinTimeUtils.LINKEDIN_ZONE_ID);

        return LocalTime.now(groupZoneId).getHour() == REMINDERS_HOUR;
    }

    private void remindMissingUser(MissingSessionUserProjection missingSessionUserProjection) {
        try {
            ChatInfo chatInfo = missingSessionUserProjectionChatInfoAdapter.adapt(missingSessionUserProjection);
            UserInfo userInfo = missingSessionUserProjectionUserInfoAdapter.adapt(missingSessionUserProjection);
            customTelegramClient.sendMessage(USER_MISSING_SESSIONS_REMINDER.formatted(FormatUtils.formatUserMention(userInfo)), chatInfo.chatId());
        } catch (Exception e) {
            backgroundErrorsCounter.increment();
            throw e;
        }
    }


}
