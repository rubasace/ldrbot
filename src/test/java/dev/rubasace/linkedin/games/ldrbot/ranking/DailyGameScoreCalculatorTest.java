package dev.rubasace.linkedin.games.ldrbot.ranking;

import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.group.ParkedPeriod;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroup;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupAdapter;
import dev.rubasace.linkedin.games.ldrbot.session.GameSession;
import dev.rubasace.linkedin.games.ldrbot.session.GameSessionService;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUser;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUserAdapter;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import dev.rubasace.linkedin.games.ldrbot.util.LinkedinTimeUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DailyGameScoreCalculatorTest {

    private final GameType GAME_TYPE = GameType.ZIP;
    private final LocalDate GAME_DATE = LocalDate.ofYearDay(1991, 232);

    private final DailyGameScoreCalculator dailyGameScoreCalculator = new DailyGameScoreCalculator();

    // Site 1's own service, for the AC-6 case at the bottom of this class. TelegramGroupService and GroupRankingService
    // are reached only from process(...), which nothing here calls, so they are left absent rather than mocked into
    // existence; both adapters are the real ones, being pure two-line mappers with no state.
    private final GameSessionService gameSessionService = mock(GameSessionService.class);
    private final GroupsRankingReadinessCheckService groupsRankingReadinessCheckService =
            new GroupsRankingReadinessCheckService(null, gameSessionService, null, new TelegramUserAdapter(), new TelegramGroupAdapter());

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

    // T1 / AC-8 — the participating count is taken at the game day of the sessions being scored. The period here is
    // CLOSED the day after GAME_DATE, so it covers that day and not today: an implementation that asked "is this player
    // parked now?" would score over four players and fail on the very first assertion.
    @Test
    void shouldScoreOverParticipatingMembersWhenSomeoneWasParkedOnThatGameDay() {

        TelegramGroup telegramGroup = new TelegramGroup();
        telegramGroup.setChatId(1L);
        telegramGroup.setGroupName("Test Group");

        List<GameSession> sessions = createSessions(telegramGroup, 8, 2, 13, 5);
        park(telegramGroup, sessions.get(2).getUser().getId(), GAME_DATE, GAME_DATE.plusDays(1));

        // Site 2 hands the calculator only the participating players' sessions; the group still holds four members.
        List<GameSession> participatingSessions = List.of(sessions.getFirst(), sessions.get(1), sessions.get(3));

        List<DailyGameScore> scores = dailyGameScoreCalculator.calculateScores(participatingSessions, telegramGroup);

        assertEquals(3, scores.size());
        assertAll(
                () -> assertEquals(4, telegramGroup.getMembers().size()),
                () -> assertScore(scores.getFirst(), sessions.get(1), telegramGroup, 1, 3),
                () -> assertScore(scores.get(1), sessions.get(3), telegramGroup, 2, 2),
                () -> assertScore(scores.get(2), sessions.getFirst(), telegramGroup, 3, 1)
        );
    }

    // T1 / AC-8, and the mirror of the case above: a player parked TODAY still counts for a past game day, so that day's
    // points stay over four. An implementation evaluating the count at todayGameDay() scores these over three.
    @Test
    void shouldScoreOverEveryMemberWhenTheParkedPeriodDoesNotCoverThatGameDay() {

        TelegramGroup telegramGroup = new TelegramGroup();
        telegramGroup.setChatId(1L);
        telegramGroup.setGroupName("Test Group");

        List<GameSession> sessions = createSessions(telegramGroup, 8, 2, 13, 5);
        park(telegramGroup, sessions.get(2).getUser().getId(), LinkedinTimeUtils.todayGameDay(), null);

        List<DailyGameScore> scores = dailyGameScoreCalculator.calculateScores(sessions, telegramGroup);

        assertEquals(4, scores.size());
        assertAll(
                () -> assertScore(scores.getFirst(), sessions.get(1), telegramGroup, 1, 4),
                () -> assertScore(scores.get(1), sessions.get(3), telegramGroup, 2, 3),
                () -> assertScore(scores.get(2), sessions.getFirst(), telegramGroup, 3, 2),
                () -> assertScore(scores.get(3), sessions.get(2), telegramGroup, 4, 1)
        );
    }

    // AC-6 / site 1 — the readiness gate, the one site none of the six bound tests reaches. B is parked on the game
    // day and has submitted nothing; A and C have submitted every tracked game. The gate must open at that point,
    // without B and without the end-of-day job. GroupsRankingReadinessCheckService is package-private to `ranking`, so
    // session/GameRecordDetectionTest cannot see it and §4.1's first harness constraint forbids opening a second
    // @SpringBootTest class to host this: that leaves this class as the only reachable host, and the mutation the case
    // has to catch (getParticipatingMembers(gameDay) back to getMembers()) lives entirely in the entity, where a real
    // database would add nothing. The false half is the seed guard: with nobody parked the SAME submissions must leave
    // the gate shut, or the case goes green over an implementation that never asks about participation at all.
    @Test
    void shouldOpenTheRankingGateWithoutWaitingForAParkedPlayer() {

        TelegramGroup telegramGroup = new TelegramGroup(-8000L, "Test Group");
        telegramGroup.setTrackedGames(EnumSet.of(GameType.QUEENS, GameType.ZIP));
        TelegramUser a = addMember(telegramGroup, -8001L);
        TelegramUser b = addMember(telegramGroup, -8002L);
        TelegramUser c = addMember(telegramGroup, -8003L);

        Map<Long, List<GameSession>> submitted = Map.of(a.getId(), everyTrackedGame(telegramGroup, a),
                                                        b.getId(), List.<GameSession>of(),
                                                        c.getId(), everyTrackedGame(telegramGroup, c));
        when(gameSessionService.getDaySessions(any(ChatInfo.class), any(UserInfo.class), eq(GAME_DATE)))
                .thenAnswer(invocation -> submitted.get(invocation.getArgument(1, UserInfo.class).id()).stream());

        park(telegramGroup, b.getId(), GAME_DATE, null);
        boolean readyWithoutTheParkedPlayer = groupsRankingReadinessCheckService.allMembersDone(telegramGroup, GAME_DATE);

        telegramGroup.getParkedPeriods().clear();
        boolean readyWithEveryMember = groupsRankingReadinessCheckService.allMembersDone(telegramGroup, GAME_DATE);

        assertAll(
                () -> assertTrue(readyWithoutTheParkedPlayer, "the ranking must not wait for a player parked on that game day"),
                () -> assertFalse(readyWithEveryMember, "the same submissions must not open the gate while that player is taking part")
        );
    }

    private TelegramUser addMember(final TelegramGroup telegramGroup, final Long id) {
        TelegramUser telegramUser = new TelegramUser();
        telegramUser.setId(id);
        telegramUser.setUserName("user" + Math.abs(id));
        telegramGroup.getMembers().add(telegramUser);
        return telegramUser;
    }

    private List<GameSession> everyTrackedGame(final TelegramGroup telegramGroup, final TelegramUser telegramUser) {
        return telegramGroup.getTrackedGames().stream()
                            .map(gameType -> {
                                GameSession gameSession = new GameSession();
                                gameSession.setGroup(telegramGroup);
                                gameSession.setUser(telegramUser);
                                gameSession.setGame(gameType);
                                gameSession.setGameDay(GAME_DATE);
                                return gameSession;
                            })
                            .toList();
    }

    private void park(final TelegramGroup telegramGroup, final Long userId, final LocalDate startGameDay, final LocalDate endGameDay) {
        ParkedPeriod parkedPeriod = new ParkedPeriod(telegramGroup, userId, startGameDay);
        parkedPeriod.setEndGameDay(endGameDay);
        telegramGroup.getParkedPeriods().add(parkedPeriod);
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