package dev.rubasace.linkedin.games_tracker.summary;

import dev.rubasace.linkedin.games_tracker.ranking.DailyGameScore;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class GameScoreDataAdapter {

    List<GameScoreData> adapt(List<DailyGameScore> dailyGameScore) {
        return dailyGameScore.stream()
                             .map(this::adapt)
                             .toList();
    }

    private GameScoreData adapt(DailyGameScore dailyGameScore) {
        return new GameScoreData(dailyGameScore.getUser().getUserName(), dailyGameScore.getGame(), dailyGameScore.getDuration(), dailyGameScore.getPoints());
    }
}
