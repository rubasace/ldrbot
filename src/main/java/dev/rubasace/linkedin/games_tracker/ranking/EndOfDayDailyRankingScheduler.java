package dev.rubasace.linkedin.games_tracker.ranking;

import dev.rubasace.linkedin.games_tracker.configuration.ExecutorsConfiguration;
import dev.rubasace.linkedin.games_tracker.util.LinkedinTimeUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class EndOfDayDailyRankingScheduler {

    private final DailyRankingRecalculationService dailyRankingRecalculationService;

    EndOfDayDailyRankingScheduler(final DailyRankingRecalculationService dailyRankingRecalculationService) {
        this.dailyRankingRecalculationService = dailyRankingRecalculationService;
        //        this.dailyRankingRecalculationService.calculateMissingRankings();
    }


    //    @Scheduled(cron = "30 0 0 * * *", zone = LinkedinTimeUtils.LINKEDIN_ZONE, scheduler = ExecutorsConfiguration.SCHEDULED_TASKS_EXECUTOR_NAME)
    @Scheduled(cron = "0 49 17 * * *", zone = LinkedinTimeUtils.LINKEDIN_ZONE, scheduler = ExecutorsConfiguration.SCHEDULED_TASKS_EXECUTOR_NAME)
    public void calculateMissingRankings() {
        dailyRankingRecalculationService.calculateMissingRankings();
    }
}
