package dev.rubasace.linkedin.games.ldrbot.summary;

import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroup;
import dev.rubasace.linkedin.games.ldrbot.ranking.DailyGameScore;
import dev.rubasace.linkedin.games.ldrbot.session.GameSession;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUser;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GroupDailyScoreAdapterTest {

    private final GroupDailyScoreAdapter groupDailyScoreAdapter = new GroupDailyScoreAdapter(new GameScoreDataAdapter(), new GlobalScoreDataAdapter());

    @Test
    void shouldAdapt() {

        long chatId = -1L;
        LocalDate gameDay = LocalDate.now();

        Map<GameType, List<DailyGameScore>> dailyGameScores = new HashMap<>();
        DailyGameScore aliceZipGameScore = createGameScore("alice", GameType.ZIP, 1, Duration.ofSeconds(2), 3);
        DailyGameScore bobZipGameScore = createGameScore("bob", GameType.ZIP, 2, Duration.ofSeconds(4), 2);
        DailyGameScore jonZipGameScore = createGameScore("jon", GameType.ZIP, 3, Duration.ofSeconds(5), 1);
        List<DailyGameScore> zipGameScores = List.of(bobZipGameScore, aliceZipGameScore, jonZipGameScore);
        dailyGameScores.put(GameType.ZIP, zipGameScores);
        DailyGameScore bobTangoGameScore = createGameScore("bob", GameType.TANGO, 1, Duration.ofSeconds(1), 3);
        DailyGameScore aliceTangoGameScore = createGameScore("alice", GameType.TANGO, 2, Duration.ofSeconds(2), 2);
        DailyGameScore jonTangoGameScore = createGameScore("jon", GameType.TANGO, 3, Duration.ofSeconds(3), 1);
        List<DailyGameScore> tangoGameScores = List.of(jonTangoGameScore, aliceTangoGameScore, bobTangoGameScore);
        dailyGameScores.put(GameType.TANGO, tangoGameScores);
        DailyGameScore aliceQueensGameScore = createGameScore("alice", GameType.QUEENS, 1, Duration.ofSeconds(2), 3);
        DailyGameScore jonQueensGameScore = createGameScore("jon", GameType.QUEENS, 2, Duration.ofSeconds(3), 2);
        DailyGameScore bobQueensGameScore = createGameScore("bob", GameType.QUEENS, 3, Duration.ofSeconds(4), 1);
        List<DailyGameScore> queensGameScores = List.of(jonQueensGameScore, aliceQueensGameScore, bobQueensGameScore);
        dailyGameScores.put(GameType.QUEENS, queensGameScores);

        GroupDailyScore groupDailyScore = groupDailyScoreAdapter.adapt(chatId, dailyGameScores, gameDay);

        assertAll(
                () -> assertGameScoreData(aliceZipGameScore, groupDailyScore.gameScores().get(GameType.ZIP).getFirst()),
                () -> assertGameScoreData(bobZipGameScore, groupDailyScore.gameScores().get(GameType.ZIP).get(1)),
                () -> assertGameScoreData(jonZipGameScore, groupDailyScore.gameScores().get(GameType.ZIP).get(2)),
                () -> assertGameScoreData(bobTangoGameScore, groupDailyScore.gameScores().get(GameType.TANGO).getFirst()),
                () -> assertGameScoreData(aliceTangoGameScore, groupDailyScore.gameScores().get(GameType.TANGO).get(1)),
                () -> assertGameScoreData(jonTangoGameScore, groupDailyScore.gameScores().get(GameType.TANGO).get(2)),
                () -> assertGameScoreData(aliceQueensGameScore, groupDailyScore.gameScores().get(GameType.QUEENS).getFirst()),
                () -> assertGameScoreData(jonQueensGameScore, groupDailyScore.gameScores().get(GameType.QUEENS).get(1)),
                () -> assertGameScoreData(bobQueensGameScore, groupDailyScore.gameScores().get(GameType.QUEENS).get(2)),
                () -> assertGlobalScore("alice", Duration.ofSeconds(6), 1, 8, groupDailyScore.globalScore().getFirst()),
                () -> assertGlobalScore("bob", Duration.ofSeconds(9), 2, 6, groupDailyScore.globalScore().get(1)),
                () -> assertGlobalScore("jon", Duration.ofSeconds(11), 3, 4, groupDailyScore.globalScore().get(2))
        );

    }

    @Test
    void shouldAdaptWithGameTies() {

        long chatId = -1L;
        LocalDate gameDay = LocalDate.now();

        Map<GameType, List<DailyGameScore>> dailyGameScores = new HashMap<>();
        DailyGameScore aliceZipGameScore = createGameScore("alice", GameType.ZIP, 1, Duration.ofSeconds(2), 3);
        DailyGameScore bobZipGameScore = createGameScore("bob", GameType.ZIP, 1, Duration.ofSeconds(2), 3);
        DailyGameScore jonZipGameScore = createGameScore("jon", GameType.ZIP, 3, Duration.ofSeconds(5), 1);
        List<DailyGameScore> zipGameScores = List.of(bobZipGameScore, aliceZipGameScore, jonZipGameScore);
        dailyGameScores.put(GameType.ZIP, zipGameScores);
        DailyGameScore bobTangoGameScore = createGameScore("bob", GameType.TANGO, 1, Duration.ofSeconds(1), 3);
        DailyGameScore aliceTangoGameScore = createGameScore("alice", GameType.TANGO, 2, Duration.ofSeconds(2), 2);
        DailyGameScore jonTangoGameScore = createGameScore("jon", GameType.TANGO, 2, Duration.ofSeconds(2), 2);
        List<DailyGameScore> tangoGameScores = List.of(jonTangoGameScore, aliceTangoGameScore, bobTangoGameScore);
        dailyGameScores.put(GameType.TANGO, tangoGameScores);
        DailyGameScore aliceQueensGameScore = createGameScore("alice", GameType.QUEENS, 1, Duration.ofSeconds(2), 3);
        DailyGameScore jonQueensGameScore = createGameScore("jon", GameType.QUEENS, 2, Duration.ofSeconds(3), 2);
        DailyGameScore bobQueensGameScore = createGameScore("bob", GameType.QUEENS, 3, Duration.ofSeconds(4), 1);
        List<DailyGameScore> queensGameScores = List.of(jonQueensGameScore, aliceQueensGameScore, bobQueensGameScore);
        dailyGameScores.put(GameType.QUEENS, queensGameScores);

        GroupDailyScore groupDailyScore = groupDailyScoreAdapter.adapt(chatId, dailyGameScores, gameDay);

        assertAll(
                () -> assertGameScoreData(bobZipGameScore, groupDailyScore.gameScores().get(GameType.ZIP).getFirst()),
                () -> assertGameScoreData(aliceZipGameScore, groupDailyScore.gameScores().get(GameType.ZIP).get(1)),
                () -> assertGameScoreData(jonZipGameScore, groupDailyScore.gameScores().get(GameType.ZIP).get(2)),
                () -> assertGameScoreData(bobTangoGameScore, groupDailyScore.gameScores().get(GameType.TANGO).getFirst()),
                () -> assertGameScoreData(jonTangoGameScore, groupDailyScore.gameScores().get(GameType.TANGO).get(1)),
                () -> assertGameScoreData(aliceTangoGameScore, groupDailyScore.gameScores().get(GameType.TANGO).get(2)),
                () -> assertGameScoreData(aliceQueensGameScore, groupDailyScore.gameScores().get(GameType.QUEENS).getFirst()),
                () -> assertGameScoreData(jonQueensGameScore, groupDailyScore.gameScores().get(GameType.QUEENS).get(1)),
                () -> assertGameScoreData(bobQueensGameScore, groupDailyScore.gameScores().get(GameType.QUEENS).get(2)),
                () -> assertGlobalScore("alice", Duration.ofSeconds(6), 1, 8, groupDailyScore.globalScore().getFirst()),
                () -> assertGlobalScore("bob", Duration.ofSeconds(7), 2, 7, groupDailyScore.globalScore().get(1)),
                () -> assertGlobalScore("jon", Duration.ofSeconds(10), 3, 5, groupDailyScore.globalScore().get(2))
        );

    }

    @Test
    void shouldAdaptWithGlobalTiesSamePointsAndDifferentDurations() {

        long chatId = -1L;
        LocalDate gameDay = LocalDate.now();

        Map<GameType, List<DailyGameScore>> dailyGameScores = new HashMap<>();
        DailyGameScore aliceZipGameScore = createGameScore("alice", GameType.ZIP, 1, Duration.ofSeconds(2), 3);
        DailyGameScore bobZipGameScore = createGameScore("bob", GameType.ZIP, 2, Duration.ofSeconds(4), 2);
        DailyGameScore jonZipGameScore = createGameScore("jon", GameType.ZIP, 3, Duration.ofSeconds(5), 1);
        List<DailyGameScore> zipGameScores = List.of(bobZipGameScore, aliceZipGameScore, jonZipGameScore);
        dailyGameScores.put(GameType.ZIP, zipGameScores);
        DailyGameScore bobTangoGameScore = createGameScore("bob", GameType.TANGO, 1, Duration.ofSeconds(1), 3);
        DailyGameScore aliceTangoGameScore = createGameScore("alice", GameType.TANGO, 2, Duration.ofSeconds(2), 2);
        DailyGameScore jonTangoGameScore = createGameScore("jon", GameType.TANGO, 3, Duration.ofSeconds(3), 1);
        List<DailyGameScore> tangoGameScores = List.of(jonTangoGameScore, aliceTangoGameScore, bobTangoGameScore);
        dailyGameScores.put(GameType.TANGO, tangoGameScores);
        DailyGameScore aliceQueensGameScore = createGameScore("alice", GameType.QUEENS, 1, Duration.ofSeconds(2), 3);
        DailyGameScore bobQueensGameScore = createGameScore("bob", GameType.QUEENS, 1, Duration.ofSeconds(2), 3);
        DailyGameScore jonQueensGameScore = createGameScore("jon", GameType.QUEENS, 3, Duration.ofSeconds(3), 2);
        List<DailyGameScore> queensGameScores = List.of(jonQueensGameScore, aliceQueensGameScore, bobQueensGameScore);
        dailyGameScores.put(GameType.QUEENS, queensGameScores);

        GroupDailyScore groupDailyScore = groupDailyScoreAdapter.adapt(chatId, dailyGameScores, gameDay);

        assertAll(
                () -> assertGameScoreData(aliceZipGameScore, groupDailyScore.gameScores().get(GameType.ZIP).getFirst()),
                () -> assertGameScoreData(bobZipGameScore, groupDailyScore.gameScores().get(GameType.ZIP).get(1)),
                () -> assertGameScoreData(jonZipGameScore, groupDailyScore.gameScores().get(GameType.ZIP).get(2)),
                () -> assertGameScoreData(bobTangoGameScore, groupDailyScore.gameScores().get(GameType.TANGO).getFirst()),
                () -> assertGameScoreData(aliceTangoGameScore, groupDailyScore.gameScores().get(GameType.TANGO).get(1)),
                () -> assertGameScoreData(jonTangoGameScore, groupDailyScore.gameScores().get(GameType.TANGO).get(2)),
                () -> assertGameScoreData(aliceQueensGameScore, groupDailyScore.gameScores().get(GameType.QUEENS).getFirst()),
                () -> assertGameScoreData(bobQueensGameScore, groupDailyScore.gameScores().get(GameType.QUEENS).get(1)),
                () -> assertGameScoreData(jonQueensGameScore, groupDailyScore.gameScores().get(GameType.QUEENS).get(2)),
                () -> assertGlobalScore("alice", Duration.ofSeconds(6), 1, 8, groupDailyScore.globalScore().getFirst()),
                () -> assertGlobalScore("bob", Duration.ofSeconds(7), 2, 8, groupDailyScore.globalScore().get(1)),
                () -> assertGlobalScore("jon", Duration.ofSeconds(11), 3, 4, groupDailyScore.globalScore().get(2))
        );

    }

    @Test
    void shouldAdaptWithGlobalTiesSamePointsAndSameDurations() {

        long chatId = -1L;
        LocalDate gameDay = LocalDate.now();

        Map<GameType, List<DailyGameScore>> dailyGameScores = new HashMap<>();
        DailyGameScore aliceZipGameScore = createGameScore("alice", GameType.ZIP, 1, Duration.ofSeconds(3), 3);
        DailyGameScore bobZipGameScore = createGameScore("bob", GameType.ZIP, 2, Duration.ofSeconds(4), 2);
        DailyGameScore jonZipGameScore = createGameScore("jon", GameType.ZIP, 3, Duration.ofSeconds(5), 1);
        List<DailyGameScore> zipGameScores = List.of(bobZipGameScore, aliceZipGameScore, jonZipGameScore);
        dailyGameScores.put(GameType.ZIP, zipGameScores);
        DailyGameScore bobTangoGameScore = createGameScore("bob", GameType.TANGO, 1, Duration.ofSeconds(1), 3);
        DailyGameScore aliceTangoGameScore = createGameScore("alice", GameType.TANGO, 2, Duration.ofSeconds(2), 2);
        DailyGameScore jonTangoGameScore = createGameScore("jon", GameType.TANGO, 3, Duration.ofSeconds(3), 1);
        List<DailyGameScore> tangoGameScores = List.of(jonTangoGameScore, aliceTangoGameScore, bobTangoGameScore);
        dailyGameScores.put(GameType.TANGO, tangoGameScores);
        DailyGameScore aliceQueensGameScore = createGameScore("alice", GameType.QUEENS, 1, Duration.ofSeconds(2), 3);
        DailyGameScore bobQueensGameScore = createGameScore("bob", GameType.QUEENS, 1, Duration.ofSeconds(2), 3);
        DailyGameScore jonQueensGameScore = createGameScore("jon", GameType.QUEENS, 3, Duration.ofSeconds(3), 2);
        List<DailyGameScore> queensGameScores = List.of(jonQueensGameScore, aliceQueensGameScore, bobQueensGameScore);
        dailyGameScores.put(GameType.QUEENS, queensGameScores);

        GroupDailyScore groupDailyScore = groupDailyScoreAdapter.adapt(chatId, dailyGameScores, gameDay);

        assertAll(
                () -> assertGameScoreData(aliceZipGameScore, groupDailyScore.gameScores().get(GameType.ZIP).getFirst()),
                () -> assertGameScoreData(bobZipGameScore, groupDailyScore.gameScores().get(GameType.ZIP).get(1)),
                () -> assertGameScoreData(jonZipGameScore, groupDailyScore.gameScores().get(GameType.ZIP).get(2)),
                () -> assertGameScoreData(bobTangoGameScore, groupDailyScore.gameScores().get(GameType.TANGO).getFirst()),
                () -> assertGameScoreData(aliceTangoGameScore, groupDailyScore.gameScores().get(GameType.TANGO).get(1)),
                () -> assertGameScoreData(jonTangoGameScore, groupDailyScore.gameScores().get(GameType.TANGO).get(2)),
                () -> assertGameScoreData(aliceQueensGameScore, groupDailyScore.gameScores().get(GameType.QUEENS).getFirst()),
                () -> assertGameScoreData(bobQueensGameScore, groupDailyScore.gameScores().get(GameType.QUEENS).get(1)),
                () -> assertGameScoreData(jonQueensGameScore, groupDailyScore.gameScores().get(GameType.QUEENS).get(2)),
                () -> assertGlobalScore("bob", Duration.ofSeconds(7), 1, 8, groupDailyScore.globalScore().getFirst()),
                () -> assertGlobalScore("alice", Duration.ofSeconds(7), 1, 8, groupDailyScore.globalScore().get(1)),
                () -> assertGlobalScore("jon", Duration.ofSeconds(11), 3, 4, groupDailyScore.globalScore().get(2))
        );

    }

    private void assertGlobalScore(final String userName, final Duration duration, final int positions, final int points, final GlobalScoreData globalScoreData) {
        assertAll(
                () -> assertEquals(userName, globalScoreData.getUserInfo().userName()),
                () -> assertEquals(duration, globalScoreData.getTotalDuration()),
                () -> assertEquals(positions, globalScoreData.getPosition()),
                () -> assertEquals(points, globalScoreData.getPoints())
        );
    }

    private void assertGameScoreData(final DailyGameScore dailyGameScore, final GameScoreData gameScoreData) {
        assertAll(
                () -> assertEquals(dailyGameScore.getUser().getUserName(), gameScoreData.userInfo().userName()),
                () -> assertEquals(dailyGameScore.getGame(), gameScoreData.game()),
                () -> assertEquals(dailyGameScore.getGameSession().getDuration(), gameScoreData.duration()),
                () -> assertEquals(dailyGameScore.getPosition(), gameScoreData.position()),
                () -> assertEquals(dailyGameScore.getPoints(), gameScoreData.points())
        );
    }

    private static DailyGameScore createGameScore(final String userName, final GameType game, final int position, final Duration duration, final int points) {
        DailyGameScore dailyGameScore = new DailyGameScore();
        GameSession gameSession = new GameSession();
        gameSession.setDuration(duration);
        dailyGameScore.setGroup(new TelegramGroup());
        TelegramUser telegramUser = new TelegramUser();
        telegramUser.setUserName(userName);
        telegramUser.setId((long) userName.hashCode());
        dailyGameScore.setUser(telegramUser);
        dailyGameScore.setGame(game);
        dailyGameScore.setPosition(position);
        dailyGameScore.setGameSession(gameSession);
        dailyGameScore.setPoints(points);
        return dailyGameScore;
    }
}