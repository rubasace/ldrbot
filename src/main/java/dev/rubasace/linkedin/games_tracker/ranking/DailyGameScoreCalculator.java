package dev.rubasace.linkedin.games_tracker.ranking;

import dev.rubasace.linkedin.games_tracker.group.TelegramGroup;
import dev.rubasace.linkedin.games_tracker.session.GameSession;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
class DailyGameScoreCalculator {

    List<DailyGameScore> calculateScores(final List<GameSession> gameSessions, final TelegramGroup group) {

        List<GameSession> sessionsByDuration = gameSessions.stream()
                                                           .sorted(Comparator.comparing(GameSession::getDuration))
                                                           .toList();


        List<DailyGameScore> dailyGameScores = new ArrayList<>();
        for (int i = 0; i < sessionsByDuration.size(); i++) {
            GameSession gameSession = sessionsByDuration.get(i);
            int points;
            if (i != 0 && gameSession.getDuration().equals(sessionsByDuration.get(i - 1).getDuration())) {
                points = dailyGameScores.get(i - 1).getPoints();
            } else {
                points = Math.max(3 - i, 0);
            }
            DailyGameScore dailyGameScore = createDailyScore(group, gameSession, points);
            dailyGameScores.add(dailyGameScore);
        }
        return dailyGameScores;
    }

    @NotNull
    private static DailyGameScore createDailyScore(final TelegramGroup group, final GameSession session, int points) {
        DailyGameScore dailyGameScore = new DailyGameScore();
        dailyGameScore.setDate(session.getGameDay());
        dailyGameScore.setUser(session.getUser());
        dailyGameScore.setGroup(group);
        dailyGameScore.setGame(session.getGame());
        dailyGameScore.setDuration(session.getDuration());
        dailyGameScore.setPoints(points);
        return dailyGameScore;
    }
}

