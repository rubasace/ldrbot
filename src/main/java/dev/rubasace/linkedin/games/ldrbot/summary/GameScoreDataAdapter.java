package dev.rubasace.linkedin.games.ldrbot.summary;

import dev.rubasace.linkedin.games.ldrbot.ranking.DailyGameScore;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
class GameScoreDataAdapter {

    List<GameScoreData> adapt(List<DailyGameScore> dailyGameScore) {
        return dailyGameScore.stream()
                             .map(this::adapt)
                             .sorted(Comparator.comparing(GameScoreData::position))
                             .toList();
    }

    private GameScoreData adapt(DailyGameScore dailyGameScore) {
        return new GameScoreData(dailyGameScore.getUser().getUserName(), dailyGameScore.getGame(), dailyGameScore.getGameSession().getDuration(), dailyGameScore.getPosition(),
                                 dailyGameScore.getPoints());
    }
}
