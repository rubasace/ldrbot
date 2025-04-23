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

    private static final int[] POINTS_PER_POSITION = new int[]{3, 2, 1};

    List<DailyGameScore> calculateScores(final List<GameSession> gameSessions, final TelegramGroup group) {

        List<GameSession> sessionsByDuration = gameSessions.stream()
                                                           .sorted(Comparator.comparing(GameSession::getDuration))
                                                           .toList();


        List<DailyGameScore> dailyGameScores = new ArrayList<>();
        for (int i = 0; i < sessionsByDuration.size(); i++) {
            GameSession gameSession = sessionsByDuration.get(i);
            int position;
            if (i != 0 && gameSession.getDuration().equals(sessionsByDuration.get(i - 1).getDuration())) {
                position = dailyGameScores.get(i - 1).getPosition();
            } else {
                position = i + 1;
            }

            DailyGameScore dailyGameScore = createDailyScore(group, gameSession, position);
            dailyGameScores.add(dailyGameScore);
        }
        return dailyGameScores;
    }

    @NotNull
    private static DailyGameScore createDailyScore(final TelegramGroup group, final GameSession session, int position) {
        DailyGameScore dailyGameScore = new DailyGameScore();
        dailyGameScore.setGameDay(session.getGameDay());
        dailyGameScore.setUser(session.getUser());
        dailyGameScore.setGroup(group);
        dailyGameScore.setGame(session.getGame());
        dailyGameScore.setGameSession(session);
        dailyGameScore.setPosition(position);
        dailyGameScore.setPoints(calculatePoints(position));
        return dailyGameScore;
    }

    private static int calculatePoints(final int position) {
        return position <= POINTS_PER_POSITION.length ? POINTS_PER_POSITION[position - 1] : 0;
    }

}

