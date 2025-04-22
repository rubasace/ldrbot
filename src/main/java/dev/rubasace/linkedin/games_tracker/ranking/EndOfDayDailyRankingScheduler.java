package dev.rubasace.linkedin.games_tracker.ranking;

import dev.rubasace.linkedin.games_tracker.group.TelegramGroup;
import dev.rubasace.linkedin.games_tracker.group.TelegramGroupService;
import dev.rubasace.linkedin.games_tracker.util.LinkedinTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
class EndOfDayDailyRankingScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndOfDayDailyRankingScheduler.class);
    private final TelegramGroupService telegramGroupService;
    private final GroupRankingService groupRankingService;

    EndOfDayDailyRankingScheduler(final TelegramGroupService telegramGroupService, final GroupRankingService groupRankingService) {
        this.telegramGroupService = telegramGroupService;
        this.groupRankingService = groupRankingService;
    }

    //    @Scheduled(cron = "30 0 0 * * *", zone = LinkedinTimeUtils.LINKEDIN_ZONE)
    @Scheduled(cron = "0 1 14 * * *", zone = LinkedinTimeUtils.LINKEDIN_ZONE)
    @Transactional
    public void runAtEndOfDay() {
        LocalDate previousGameDay = LinkedinTimeUtils.todayGameDay();
        telegramGroupService.getGroupsWithoutDailyRanking(previousGameDay)
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
