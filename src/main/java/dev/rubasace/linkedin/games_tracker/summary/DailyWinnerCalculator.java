package dev.rubasace.linkedin.games_tracker.summary;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
class DailyWinnerCalculator {

    List<String> getWinners(final List<GlobalScoreData> globalScoreData) {
        if (globalScoreData.isEmpty()) {
            return List.of();
        }
        GlobalScoreData first = globalScoreData.getFirst();
        return globalScoreData.stream()
                              .takeWhile(score -> isATie(score, first))
                              .map(GlobalScoreData::username)
                              .toList();
    }

    private static boolean isATie(final GlobalScoreData score, final GlobalScoreData first) {
        return score.points() == first.points() &&
                score.totalDuration().equals(first.totalDuration());
    }

}
