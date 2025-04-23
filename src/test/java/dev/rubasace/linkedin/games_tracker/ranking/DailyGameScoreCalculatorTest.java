package dev.rubasace.linkedin.games_tracker.ranking;

import dev.rubasace.linkedin.games_tracker.group.TelegramGroup;
import dev.rubasace.linkedin.games_tracker.session.GameSession;
import dev.rubasace.linkedin.games_tracker.session.GameType;
import dev.rubasace.linkedin.games_tracker.user.TelegramUser;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

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

        GameSession first = createSession("Dave", Duration.ofSeconds(3));
        GameSession second = createSession("Charlie", Duration.ofSeconds(8));
        GameSession third = createSession("Alice", Duration.ofSeconds(12));
        GameSession fourth = createSession("Bob", Duration.ofSeconds(15));
        GameSession fifth = createSession("Eve", Duration.ofSeconds(20));

        List<GameSession> sessions = List.of(third, fourth, second, first, fifth);

        List<DailyGameScore> scores = dailyGameScoreCalculator.calculateScores(sessions, telegramGroup);

        assertEquals(5, scores.size());
        assertAll(
                () -> assertScore(scores.getFirst(), first, telegramGroup, 1, 3),
                () -> assertScore(scores.get(1), second, telegramGroup, 2, 2),
                () -> assertScore(scores.get(2), third, telegramGroup, 3, 1),
                () -> assertScore(scores.get(3), fourth, telegramGroup, 4, 0),
                () -> assertScore(scores.get(4), fifth, telegramGroup, 5, 0)
        );
    }

    @Test
    void shouldScoreWhenTiedOnTop() {

        TelegramGroup telegramGroup = new TelegramGroup();
        telegramGroup.setChatId(1L);
        telegramGroup.setGroupName("Test Group");

        GameSession first = createSession("Dave", Duration.ofSeconds(4));
        GameSession first2 = createSession("Charlie", Duration.ofSeconds(4));
        GameSession third = createSession("Eve", Duration.ofSeconds(5));
        GameSession fourth = createSession("Alice", Duration.ofSeconds(8));
        GameSession fifith = createSession("Bob", Duration.ofSeconds(9));

        List<GameSession> sessions = List.of(fourth, fifith, first, first2, third);

        List<DailyGameScore> scores = dailyGameScoreCalculator.calculateScores(sessions, telegramGroup);

        assertEquals(5, scores.size());
        assertAll(
                () -> assertScore(scores.getFirst(), first, telegramGroup, 1, 3),
                () -> assertScore(scores.get(1), first2, telegramGroup, 1, 3),
                () -> assertScore(scores.get(2), third, telegramGroup, 3, 1),
                () -> assertScore(scores.get(3), fourth, telegramGroup, 4, 0),
                () -> assertScore(scores.get(4), fifith, telegramGroup, 5, 0)
        );
    }

    @Test
    void shouldScoreWhenTiedOnMiddle() {

        TelegramGroup telegramGroup = new TelegramGroup();
        telegramGroup.setChatId(1L);
        telegramGroup.setGroupName("Test Group");

        GameSession first = createSession("Dave", Duration.ofSeconds(5));
        GameSession first2 = createSession("Charlie", Duration.ofSeconds(5));
        GameSession third = createSession("Eve", Duration.ofSeconds(8));
        GameSession third2 = createSession("Alice", Duration.ofSeconds(8));
        GameSession fifith = createSession("Bob", Duration.ofSeconds(13));

        List<GameSession> sessions = List.of(third, fifith, first, first2, third2);

        List<DailyGameScore> scores = dailyGameScoreCalculator.calculateScores(sessions, telegramGroup);

        assertEquals(5, scores.size());
        assertAll(
                () -> assertScore(scores.getFirst(), first, telegramGroup, 1, 3),
                () -> assertScore(scores.get(1), first2, telegramGroup, 1, 3),
                () -> assertScore(scores.get(2), third, telegramGroup, 3, 1),
                () -> assertScore(scores.get(3), third2, telegramGroup, 3, 1),
                () -> assertScore(scores.get(4), fifith, telegramGroup, 5, 0)
        );
    }


    @Test
    void shouldScoreWhenTiedOnBottom() {

        TelegramGroup telegramGroup = new TelegramGroup();
        telegramGroup.setChatId(1L);
        telegramGroup.setGroupName("Test Group");

        GameSession first = createSession("Dave", Duration.ofSeconds(2));
        GameSession second = createSession("Charlie", Duration.ofSeconds(5));
        GameSession third = createSession("Eve", Duration.ofSeconds(8));
        GameSession third2 = createSession("Alice", Duration.ofSeconds(8));
        GameSession fifth = createSession("Bob", Duration.ofSeconds(13));

        List<GameSession> sessions = List.of(third, fifth, first, second, third2);

        List<DailyGameScore> scores = dailyGameScoreCalculator.calculateScores(sessions, telegramGroup);

        assertEquals(5, scores.size());
        assertAll(
                () -> assertScore(scores.getFirst(), first, telegramGroup, 1, 3),
                () -> assertScore(scores.get(1), second, telegramGroup, 2, 2),
                () -> assertScore(scores.get(2), third, telegramGroup, 3, 1),
                () -> assertScore(scores.get(3), third2, telegramGroup, 3, 1),
                () -> assertScore(scores.get(4), fifth, telegramGroup, 5, 0)
        );
    }


    @Test
    void shouldScoreWhenTiedNoScore() {

        TelegramGroup telegramGroup = new TelegramGroup();
        telegramGroup.setChatId(1L);
        telegramGroup.setGroupName("Test Group");

        GameSession first = createSession("Dave", Duration.ofSeconds(6));
        GameSession second = createSession("Charlie", Duration.ofSeconds(9));
        GameSession third = createSession("Eve", Duration.ofSeconds(10));
        GameSession fourth = createSession("Alice", Duration.ofSeconds(11));
        GameSession fourth2 = createSession("Bob", Duration.ofSeconds(11));

        List<GameSession> sessions = List.of(third, fourth, first, second, fourth2);

        List<DailyGameScore> scores = dailyGameScoreCalculator.calculateScores(sessions, telegramGroup);

        assertEquals(5, scores.size());
        assertAll(
                () -> assertScore(scores.getFirst(), first, telegramGroup, 1, 3),
                () -> assertScore(scores.get(1), second, telegramGroup, 2, 2),
                () -> assertScore(scores.get(2), third, telegramGroup, 3, 1),
                () -> assertScore(scores.get(3), fourth, telegramGroup, 4, 0),
                () -> assertScore(scores.get(4), fourth2, telegramGroup, 4, 0)
        );
    }

    private void assertScore(DailyGameScore score, GameSession gameSession, final TelegramGroup group, final int expectedPosition, int expectedPoints) {
        assertAll(
                () -> assertEquals(gameSession.getUser().getUserName(), score.getUser().getUserName()),
                () -> assertEquals(gameSession.getGame(), score.getGame()),
                () -> assertEquals(group, score.getGroup()),
                () -> assertEquals(gameSession.getGameDay(), score.getDate()),
                () -> assertEquals(expectedPosition, score.getPosition()),
                () -> assertEquals(expectedPoints, score.getPoints())
        );
    }

    private GameSession createSession(final String username, final Duration duration) {
        TelegramUser user = new TelegramUser();
        user.setId(new Random().nextLong());
        user.setUserName(username);

        GameSession session = new GameSession();
        session.setUser(user);
        session.setGame(GAME_TYPE);
        session.setGameDay(GAME_DATE);
        session.setDuration(duration);
        return session;
    }
}