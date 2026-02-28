package dev.rubasace.linkedin.games.ldrbot.ranking;

import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroup;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupAdapter;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupService;
import dev.rubasace.linkedin.games.ldrbot.metrics.MetricsConstants;
import dev.rubasace.linkedin.games.ldrbot.util.BackpressureExecutors;
import dev.rubasace.linkedin.games.ldrbot.util.LinkedinTimeUtils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.concurrent.ExecutorService;

@Transactional(readOnly = true)
@Service
public class DailyRankingRecalculationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DailyRankingRecalculationService.class);
    public static final int MAX_CONCURRENCY = 20;
    private static final String TASK_NAME = "ranking";

    private final TelegramGroupService telegramGroupService;
    private final GroupRankingService groupRankingService;
    private final ExecutorService executorService;
    private final TelegramGroupAdapter telegramGroupAdapter;
    private final MeterRegistry meterRegistry;
    private final Timer backgroundDurationTimer;
    private final Counter backgroundErrorsCounter;

    DailyRankingRecalculationService(final TelegramGroupService telegramGroupService,
                                     final GroupRankingService groupRankingService,
                                     final TelegramGroupAdapter telegramGroupAdapter,
                                     final MeterRegistry meterRegistry) {
        this.telegramGroupService = telegramGroupService;
        this.groupRankingService = groupRankingService;
        this.telegramGroupAdapter = telegramGroupAdapter;
        this.executorService = BackpressureExecutors.newBackPressureVirtualThreadPerTaskExecutor("ranking", MAX_CONCURRENCY);
        this.meterRegistry = meterRegistry;
        this.backgroundDurationTimer = meterRegistry.timer(MetricsConstants.BACKGROUND_DURATION,
                MetricsConstants.TAG_TASK_NAME, TASK_NAME);
        this.backgroundErrorsCounter = meterRegistry.counter(MetricsConstants.BACKGROUND_ERRORS,
                MetricsConstants.TAG_TASK_NAME, TASK_NAME);
    }


    public void calculateMissingRankings() {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            LocalDate previousGameDay = LinkedinTimeUtils.todayGameDay().minusDays(1);
            telegramGroupService.findGroupsWithMissingScores(previousGameDay)
                                .forEach(telegramGroup -> executorService.execute(() -> generateDailyRanking(telegramGroup, previousGameDay)));
        } finally {
            sample.stop(backgroundDurationTimer);
        }
    }

    private void generateDailyRanking(TelegramGroup telegramGroup, final LocalDate gameDay) {
        try {
            ChatInfo chatInfo = telegramGroupAdapter.adapt(telegramGroup);
            groupRankingService.createDailyRanking(chatInfo, gameDay);
        } catch (Exception e) {
            backgroundErrorsCounter.increment();
            LOGGER.error("Failed to generate daily ranking for group {}, error message: {}", telegramGroup.getChatId(), e.getMessage(), e);
        }
    }
}
