package dev.rubasace.linkedin.games_tracker.scheduled;

import dev.rubasace.linkedin.games_tracker.configuration.ExecutorsConfiguration;
import dev.rubasace.linkedin.games_tracker.ranking.DailyRankingRecalculationService;
import dev.rubasace.linkedin.games_tracker.util.LinkedinTimeUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class EndOfDayDailyRankingScheduler implements ApplicationListener<ApplicationReadyEvent> {

    private final DailyRankingRecalculationService dailyRankingRecalculationService;

    EndOfDayDailyRankingScheduler(final DailyRankingRecalculationService dailyRankingRecalculationService) {
        this.dailyRankingRecalculationService = dailyRankingRecalculationService;
    }

    @Scheduled(cron = "30 0 0 * * *", zone = LinkedinTimeUtils.LINKEDIN_ZONE, scheduler = ExecutorsConfiguration.SCHEDULED_TASKS_EXECUTOR_NAME)
    public void calculateMissingRankings() {
        dailyRankingRecalculationService.calculateMissingRankings();
    }

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        this.dailyRankingRecalculationService.calculateMissingRankings();
    }
}
