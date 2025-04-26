package dev.rubasace.linkedin.games.ldrbot.ranking;

import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroup;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupAdapter;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupService;
import dev.rubasace.linkedin.games.ldrbot.util.BackpressureExecutors;
import dev.rubasace.linkedin.games.ldrbot.util.LinkedinTimeUtils;
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
    private final TelegramGroupService telegramGroupService;
    private final GroupRankingService groupRankingService;
    private final ExecutorService executorService;
    private final TelegramGroupAdapter telegramGroupAdapter;

    DailyRankingRecalculationService(final TelegramGroupService telegramGroupService, final GroupRankingService groupRankingService, final TelegramGroupAdapter telegramGroupAdapter) {
        this.telegramGroupService = telegramGroupService;
        this.groupRankingService = groupRankingService;
        this.telegramGroupAdapter = telegramGroupAdapter;
        executorService = BackpressureExecutors.newBackPressureVirtualThreadPerTaskExecutor("ranking-recaulculation", MAX_CONCURRENCY);
    }


    public void calculateMissingRankings() {
        LocalDate previousGameDay = LinkedinTimeUtils.todayGameDay().minusDays(1);
        telegramGroupService.findGroupsWithMissingScores(previousGameDay)
                            .forEach(telegramGroup -> executorService.execute(() -> generateDailyRanking(telegramGroup, previousGameDay)));
    }

    private void generateDailyRanking(TelegramGroup telegramGroup, final LocalDate gameDay) {
        try {
            ChatInfo chatInfo = telegramGroupAdapter.adapt(telegramGroup);
            groupRankingService.createDailyRanking(chatInfo, gameDay);
        } catch (Exception e) {
            LOGGER.error("Failed to generate daily ranking for group {}, error message: {}", telegramGroup.getChatId(), e.getMessage(), e);
        }
    }
}
