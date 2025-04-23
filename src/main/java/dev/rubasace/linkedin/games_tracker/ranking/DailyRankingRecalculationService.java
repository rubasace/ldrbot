package dev.rubasace.linkedin.games_tracker.ranking;

import dev.rubasace.linkedin.games_tracker.group.TelegramGroup;
import dev.rubasace.linkedin.games_tracker.group.TelegramGroupService;
import dev.rubasace.linkedin.games_tracker.util.LinkedinTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
class DailyRankingRecalculationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DailyRankingRecalculationService.class);
    private final TelegramGroupService telegramGroupService;
    private final GroupRankingService groupRankingService;

    DailyRankingRecalculationService(final TelegramGroupService telegramGroupService, final GroupRankingService groupRankingService) {
        this.telegramGroupService = telegramGroupService;
        this.groupRankingService = groupRankingService;
    }

    //TODO offload into separated transactions (one per group) and parallelize with an executor
    @Transactional
    void calculateMissingRankings() {
        LocalDate previousGameDay = LinkedinTimeUtils.todayGameDay().minusDays(1);
        telegramGroupService.findGroupsWithMissingScores(previousGameDay)
                            .forEach(telegramGroup -> generateDailyRanking(telegramGroup, previousGameDay));
    }

    private void generateDailyRanking(TelegramGroup telegramGroup, final LocalDate gameDay) {
        try {
            groupRankingService.createDailyRanking(telegramGroup, gameDay);
        } catch (Exception e) {
            LOGGER.error("Failed to generate daily ranking for group {}, error message: {}", telegramGroup.getChatId(), e.getMessage(), e);
        }
    }
}
