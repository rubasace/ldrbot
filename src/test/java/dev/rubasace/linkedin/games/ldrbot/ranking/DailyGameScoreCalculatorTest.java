package dev.rubasace.linkedin.games.ldrbot.ranking;

import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroup;
import dev.rubasace.linkedin.games.ldrbot.session.GameSession;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUser;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DailyGameScoreCalculatorTest {

    private final GameType GAME_TYPE = GameType.ZIP;
    private final LocalDate GAME_DATE = LocalDate.ofYearDay(1991, 232);

    private final DailyGameScoreCalculator dailyGameScoreCalculator = new DailyGameScoreCalculator();

    @Test
    void shouldScore() {

        TelegramGroup telegramGroup = new TelegramGroup();
        telegramGroup.setChatId(1L);
        telegramGroup.setGroupName("Test Group");

        List<GameSession> sessions = createSessions(telegramGroup, 8, 2, 13, 5, 4);

        List<DailyGameScore> scores = dailyGameScoreCalculator.calculateScores(sessions, telegramGroup);

        assertEquals(sessions.size(), scores.size());
        assertAll(
                () -> assertScore(scores.getFirst(), sessions.get(1), telegramGroup, 1, 5),
                () -> assertScore(scores.get(1), sessions.get(4), telegramGroup, 2, 4),
                () -> assertScore(scores.get(2), sessions.get(3), telegramGroup, 3, 3),
                () -> assertScore(scores.get(3), sessions.getFirst(), telegramGroup, 4, 2),
                () -> assertScore(scores.get(4), sessions.get(2), telegramGroup, 5, 1)
        );
    }

    @NotNull
    private static TelegramUser getE() {
        return new TelegramUser();
    }

    @Test
    void shouldScoreAtLeastOnePoint() {

        TelegramGroup telegramGroup = new TelegramGroup();
        telegramGroup.setChatId(1L);
        telegramGroup.setGroupName("Test Group");

        List<GameSession> sessions = createSessions(telegramGroup, 8, 2, 13, 5, 4, 15, 19);

        List<DailyGameScore> scores = dailyGameScoreCalculator.calculateScores(sessions, telegramGroup);

        assertEquals(sessions.size(), scores.size());
        assertAll(
                () -> assertScore(scores.getFirst(), sessions.get(1), telegramGroup, 1, 7),
                () -> assertScore(scores.get(1), sessions.get(4), telegramGroup, 2, 6),
                () -> assertScore(scores.get(2), sessions.get(3), telegramGroup, 3, 5),
                () -> assertScore(scores.get(3), sessions.getFirst(), telegramGroup, 4, 4),
                () -> assertScore(scores.get(4), sessions.get(2), telegramGroup, 5, 3),
                () -> assertScore(scores.get(5), sessions.get(5), telegramGroup, 6, 2),
                () -> assertScore(scores.get(6), sessions.get(6), telegramGroup, 7, 1)
        );
    }

    @Test
    void shouldScoreWhenTiedOnTop() {

        TelegramGroup telegramGroup = new TelegramGroup();
        telegramGroup.setChatId(1L);
        telegramGroup.setGroupName("Test Group");

        List<GameSession> sessions = createSessions(telegramGroup, 8, 2, 13, 2, 8);

        List<DailyGameScore> scores = dailyGameScoreCalculator.calculateScores(sessions, telegramGroup);

        assertEquals(sessions.size(), scores.size());
        assertAll(
                () -> assertScore(scores.getFirst(), sessions.get(1), telegramGroup, 1, 5),
                () -> assertScore(scores.get(1), sessions.get(3), telegramGroup, 1, 5),
                () -> assertScore(scores.get(2), sessions.getFirst(), telegramGroup, 3, 3),
                () -> assertScore(scores.get(3), sessions.get(4), telegramGroup, 3, 3),
                () -> assertScore(scores.get(4), sessions.get(2), telegramGroup, 5, 1)
        );
    }

    @Test
    void shouldScoreWhenTiedOnMiddle() {

        TelegramGroup telegramGroup = new TelegramGroup();
        telegramGroup.setChatId(1L);
        telegramGroup.setGroupName("Test Group");

        List<GameSession> sessions = createSessions(telegramGroup, 8, 2, 5, 5, 11);

        List<DailyGameScore> scores = dailyGameScoreCalculator.calculateScores(sessions, telegramGroup);

        assertEquals(sessions.size(), scores.size());
        assertAll(
                () -> assertScore(scores.getFirst(), sessions.get(1), telegramGroup, 1, 5),
                () -> assertScore(scores.get(1), sessions.get(2), telegramGroup, 2, 4),
                () -> assertScore(scores.get(2), sessions.get(3), telegramGroup, 2, 4),
                () -> assertScore(scores.get(3), sessions.getFirst(), telegramGroup, 4, 2),
                () -> assertScore(scores.get(4), sessions.get(4), telegramGroup, 5, 1)
        );
    }


    @Test
    void shouldScoreWhenTiedOnBottom() {

        TelegramGroup telegramGroup = new TelegramGroup();
        telegramGroup.setChatId(1L);
        telegramGroup.setGroupName("Test Group");

        List<GameSession> sessions = createSessions(telegramGroup, 8, 2, 1, 5, 8);

        List<DailyGameScore> scores = dailyGameScoreCalculator.calculateScores(sessions, telegramGroup);

        assertEquals(sessions.size(), scores.size());
        assertAll(
                () -> assertScore(scores.getFirst(), sessions.get(2), telegramGroup, 1, 5),
                () -> assertScore(scores.get(1), sessions.get(1), telegramGroup, 2, 4),
                () -> assertScore(scores.get(2), sessions.get(3), telegramGroup, 3, 3),
                () -> assertScore(scores.get(3), sessions.getFirst(), telegramGroup, 4, 2),
                () -> assertScore(scores.get(4), sessions.get(4), telegramGroup, 4, 2)
        );
    }

    private void assertScore(DailyGameScore score, GameSession gameSession, final TelegramGroup group, final int expectedPosition, int expectedPoints) {
        assertAll(
                () -> assertEquals(gameSession.getUser().getUserName(), score.getUser().getUserName()),
                () -> assertEquals(gameSession.getGame(), score.getGame()),
                () -> assertEquals(group, score.getGroup()),
                () -> assertEquals(gameSession.getGameDay(), score.getGameDay()),
                () -> assertEquals(expectedPosition, score.getPosition()),
                () -> assertEquals(expectedPoints, score.getPoints())
        );
    }

    private List<GameSession> createSessions(final TelegramGroup telegramGroup, final int... seconds) {
        return Arrays.stream(seconds)
                     .mapToObj(s -> createSession(s, telegramGroup))
                     .toList();
    }

    private GameSession createSession(final int seconds, final TelegramGroup telegramGroup) {
        TelegramUser user = new TelegramUser();
        user.setId(new Random().nextLong());
        user.setUserName(UUID.randomUUID().toString());
        telegramGroup.getMembers().add(user);
        GameSession session = new GameSession();
        session.setUser(user);
        session.setGame(GAME_TYPE);
        session.setGameDay(GAME_DATE);
        session.setDuration(Duration.ofSeconds(seconds));
        session.setGroup(telegramGroup);
        return session;
    }
}