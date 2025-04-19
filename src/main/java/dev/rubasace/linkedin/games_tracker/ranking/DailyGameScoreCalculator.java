package dev.rubasace.linkedin.games_tracker.ranking;

import dev.rubasace.linkedin.games_tracker.group.TelegramGroup;
import dev.rubasace.linkedin.games_tracker.session.GameSession;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
class DailyGameScoreCalculator {

    List<DailyGameScore> calculateScores(final List<GameSession> gameSessions, final TelegramGroup group) {

        List<GameSession> sessionsByDuration = gameSessions.stream()
                                                           .sorted(Comparator.comparing(GameSession::getDuration))
                                                           .toList();
        List<Duration> durations = gameSessions.stream()
                                               .map(GameSession::getDuration)
                                               .distinct()
                                               .sorted()
                                               .toList();


        List<DailyGameScore> dailyGameScores = new ArrayList<>();
        for (int i = 0; i < sessionsByDuration.size(); i++) {
            GameSession gameSession = sessionsByDuration.get(i);
            DailyGameScore dailyGameScore = createDailyScore(group, gameSession, calculatePoints(gameSession, durations));
            dailyGameScores.add(dailyGameScore);
        }
        return dailyGameScores;
    }

    private int calculatePoints(GameSession gameSession, List<Duration> durations) {
        int index = durations.indexOf(gameSession.getDuration());
        return Math.max(0, 3 - index);
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

