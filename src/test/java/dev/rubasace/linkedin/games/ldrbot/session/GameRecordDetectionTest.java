package dev.rubasace.linkedin.games.ldrbot.session;

import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.group.GroupNotFoundException;
import dev.rubasace.linkedin.games.ldrbot.group.ParkedPeriod;
import dev.rubasace.linkedin.games.ldrbot.group.PlayerParticipationChangedEvent;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroup;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupRepository;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupService;
import dev.rubasace.linkedin.games.ldrbot.ranking.DailyGameScore;
import dev.rubasace.linkedin.games.ldrbot.ranking.DailyScoreRepository;
import dev.rubasace.linkedin.games.ldrbot.user.MissingSessionUserProjection;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUser;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUserRepository;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import dev.rubasace.linkedin.games.ldrbot.util.LinkedinTimeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.telegram.telegrambots.longpolling.starter.TelegramBotInitializer;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the real {@link GameSessionService#recordGameSession} and asserts on the recorded
 * {@link GameRecordEstablishedEvent}s. Deliberately NOT {@code @Transactional}: the detection query runs in its own
 * REQUIRES_NEW transaction on a separate connection and cannot see an uncommitted outer transaction, so under
 * {@code @Transactional} every announce would degrade to the first-record shape and the silent rows would announce.
 * The bean overrides are exactly {@code ApplicationTests}' so both classes share one application context — a second
 * context that instantiates {@code MessageController} fails on its exclusive MapDB file lock.
 */
@SpringBootTest
@RecordApplicationEvents
class GameRecordDetectionTest {

    private static final Long RECORD_GROUP_CHAT_ID = -900001L;
    private static final Long OTHER_GROUP_CHAT_ID = -900002L;
    private static final Long UNTRACKED_GROUP_CHAT_ID = -900003L;

    private static final Long T3_GROUP_CHAT_ID = -900101L;
    private static final Long T3_OTHER_GROUP_CHAT_ID = -900102L;
    private static final Long T4_A_CHAT_ID = -900201L;
    private static final Long T4_B_CHAT_ID = -900202L;
    private static final Long T4_C_CHAT_ID = -900203L;
    private static final Long T4_D_CHAT_ID = -900204L;
    private static final Long T4_E_CHAT_ID = -900205L;
    private static final Long T4_F_CHAT_ID = -900206L;
    private static final Long T4_G_CHAT_ID = -900207L;
    private static final Long T5_CHAT_ID = -900301L;
    private static final Long T5_LAST_CHAT_ID = -900302L;

    private static final GameType GAME = GameType.QUEENS;
    private static final LocalDate GAME_DAY = LocalDate.of(2025, 3, 10);
    private static final LocalDate OLDER_GAME_DAY = LocalDate.of(2025, 2, 3);
    private static final LocalDate T3_GAME_DAY = LocalDate.of(2024, 5, 20);
    private static final LocalDate T4_GAME_DAY = LocalDate.of(2024, 6, 15);

    private static final UserInfo ADA = new UserInfo(-9001L, "ada", "Ada", "Lovelace");
    private static final UserInfo BOB = new UserInfo(-9002L, "bob", "Bob", "Babbage");

    @MockitoBean
    private TelegramBotInitializer telegramBotInitializer;

    @Autowired
    private GameSessionService gameSessionService;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private TelegramGroupRepository telegramGroupRepository;

    @Autowired
    private TelegramUserRepository telegramUserRepository;

    @Autowired
    private DailyScoreRepository dailyScoreRepository;

    @Autowired
    private TelegramGroupService telegramGroupService;

    @Autowired
    private ApplicationEvents applicationEvents;

    @BeforeEach
    void resetSessions() {
        gameSessionRepository.deleteAll();
        seedGroup(RECORD_GROUP_CHAT_ID, EnumSet.allOf(GameType.class));
        seedGroup(OTHER_GROUP_CHAT_ID, EnumSet.allOf(GameType.class));
        seedGroup(UNTRACKED_GROUP_CHAT_ID, EnumSet.complementOf(EnumSet.of(GAME)));
    }

    // Row 1 / AC2 — the first-ever row for this group+game announces with no other time to report.
    @Test
    void shouldAnnounceWithNullBestOtherOnFirstEverRow() throws Exception {
        submit(RECORD_GROUP_CHAT_ID, ADA, 100, GAME_DAY, false);

        List<GameRecordEstablishedEvent> events = recordEvents();
        assertEquals(1, events.size());
        assertAll(
                () -> assertNull(events.getFirst().getBestOtherDuration()),
                () -> assertEquals(Duration.ofSeconds(100), events.getFirst().getDuration()),
                () -> assertEquals(RECORD_GROUP_CHAT_ID, events.getFirst().getChatInfo().chatId()),
                () -> assertEquals("Queens", events.getFirst().getGameInfo().name())
        );
    }

    // Row 2 / AC1, AC3 — a new row strictly faster than the standing best announces and reports that best.
    @Test
    void shouldAnnounceWithBestOtherWhenNewRowBeatsTheRecord() throws Exception {
        submit(RECORD_GROUP_CHAT_ID, ADA, 100, GAME_DAY, false);
        submit(RECORD_GROUP_CHAT_ID, BOB, 70, GAME_DAY, false);

        List<GameRecordEstablishedEvent> events = recordEvents();
        assertEquals(2, events.size());
        assertAll(
                () -> assertEquals(Duration.ofSeconds(70), events.getLast().getDuration()),
                () -> assertEquals(Duration.ofSeconds(100), events.getLast().getBestOtherDuration())
        );
    }

    // Row 3 / AC4 — a new row slower than the standing best is silent.
    @Test
    void shouldBeSilentWhenNewRowIsSlowerThanTheRecord() throws Exception {
        submit(RECORD_GROUP_CHAT_ID, ADA, 70, GAME_DAY, false);
        submit(RECORD_GROUP_CHAT_ID, BOB, 100, GAME_DAY, false);

        assertEquals(1, recordEvents().size());
    }

    // Row 4 / AC5 — an exactly-equal new row is silent: the comparison is strictly <, never <=.
    @Test
    void shouldBeSilentWhenNewRowTiesTheRecord() throws Exception {
        submit(RECORD_GROUP_CHAT_ID, ADA, 70, GAME_DAY, false);
        submit(RECORD_GROUP_CHAT_ID, BOB, 70, GAME_DAY, false);

        assertEquals(1, recordEvents().size());
    }

    // Row 5a / AC12, AC3 — d(50) < B(70) < dPrev(100): the announced value is B, never the submitter's own
    // superseded time, which is gone from the table. Three distinct values also pin the capture ordering:
    // reading the row's duration after setDuration would make dPrev == d and suppress this event entirely.
    @Test
    void shouldReportBestOtherAndNotOwnSupersededTimeWhenOverrideBeatsTheRecord() throws Exception {
        submit(RECORD_GROUP_CHAT_ID, ADA, 100, GAME_DAY, false);
        submit(RECORD_GROUP_CHAT_ID, BOB, 70, GAME_DAY, false);
        submit(RECORD_GROUP_CHAT_ID, ADA, 50, GAME_DAY, true);

        List<GameRecordEstablishedEvent> events = recordEvents();
        assertEquals(3, events.size());
        assertAll(
                () -> assertEquals(Duration.ofSeconds(50), events.getLast().getDuration()),
                () -> assertEquals(Duration.ofSeconds(70), events.getLast().getBestOtherDuration()),
                () -> assertNotEquals(Duration.ofSeconds(100), events.getLast().getBestOtherDuration())
        );
    }

    // Row 5b / AC12 — d(40) < dPrev(50) < B(70): the submitter already held the record, so the announced B is the
    // next best time and NOT the previous record. Named accepted deviation from a literal AC3 (contract §4.5.3).
    @Test
    void shouldReportNextBestTimeWhenOverridingOwnStandingRecord() throws Exception {
        submit(RECORD_GROUP_CHAT_ID, ADA, 50, GAME_DAY, false);
        submit(RECORD_GROUP_CHAT_ID, BOB, 70, GAME_DAY, false);
        submit(RECORD_GROUP_CHAT_ID, ADA, 40, GAME_DAY, true);

        List<GameRecordEstablishedEvent> events = recordEvents();
        assertEquals(2, events.size());
        assertAll(
                () -> assertEquals(Duration.ofSeconds(40), events.getLast().getDuration()),
                () -> assertEquals(Duration.ofSeconds(70), events.getLast().getBestOtherDuration())
        );
    }

    // Row 7 / AC13 — overriding one's own row to a slower time is silent while other rows exist.
    @Test
    void shouldBeSilentWhenOverrideIsSlowerAndOtherRowsExist() throws Exception {
        submit(RECORD_GROUP_CHAT_ID, BOB, 70, GAME_DAY, false);
        submit(RECORD_GROUP_CHAT_ID, ADA, 50, GAME_DAY, false);
        submit(RECORD_GROUP_CHAT_ID, ADA, 60, GAME_DAY, true);

        assertEquals(2, recordEvents().size());
    }

    // Row 8 / AC13 — the sole-row case. In committed state this is byte-identical to row 1, so only the captured
    // pre-override duration can tell them apart.
    @Test
    void shouldBeSilentWhenOverrideIsSlowerOnTheSoleRow() throws Exception {
        submit(RECORD_GROUP_CHAT_ID, ADA, 50, GAME_DAY, false);
        submit(RECORD_GROUP_CHAT_ID, ADA, 60, GAME_DAY, true);

        assertEquals(1, recordEvents().size());
    }

    // AC10 — detection spans all game days. Backfilling an older day still sets a record, and the announced best
    // other time is the newer day's row: this fails the moment a gameDay predicate is added to the detection query.
    @Test
    void shouldAnnounceWhenOverrideBackfillsAnOlderGameDay() throws Exception {
        submit(RECORD_GROUP_CHAT_ID, ADA, 100, GAME_DAY, false);
        submit(RECORD_GROUP_CHAT_ID, ADA, 50, OLDER_GAME_DAY, true);

        List<GameRecordEstablishedEvent> events = recordEvents();
        assertEquals(2, events.size());
        assertAll(
                () -> assertEquals(Duration.ofSeconds(50), events.getLast().getDuration()),
                () -> assertEquals(Duration.ofSeconds(100), events.getLast().getBestOtherDuration())
        );
    }

    // AC11 — detection is group-scoped: a faster time in another group neither notifies this group nor counts as
    // this group's best.
    @Test
    void shouldNotLetAnotherGroupsTimeCountAsThisGroupsRecord() throws Exception {
        submit(OTHER_GROUP_CHAT_ID, ADA, 30, GAME_DAY, false);
        submit(RECORD_GROUP_CHAT_ID, ADA, 100, GAME_DAY, false);

        List<GameRecordEstablishedEvent> recordGroupEvents = recordEvents(RECORD_GROUP_CHAT_ID);
        assertEquals(1, recordGroupEvents.size());
        assertAll(
                () -> assertNull(recordGroupEvents.getFirst().getBestOtherDuration()),
                () -> assertEquals(Duration.ofSeconds(100), recordGroupEvents.getFirst().getDuration()),
                () -> assertEquals(1, recordEvents(OTHER_GROUP_CHAT_ID).size())
        );
    }

    // AC14 — a game the group does not track produces no row and therefore no record event.
    @Test
    void shouldNotAnnounceForAnUntrackedGame() throws Exception {
        submit(UNTRACKED_GROUP_CHAT_ID, ADA, 10, GAME_DAY, false);

        assertAll(
                () -> assertTrue(recordEvents().isEmpty()),
                () -> assertEquals(0, gameSessionRepository.count())
        );
    }

    // AC15 — the mechanism guard. Resolved through the attribute source the repository proxy actually uses, so a
    // future Spring Data precedence change that silently reverts this query to the caller's transaction is caught.
    // If this ever goes red, REPAIR it; deleting it removes the only check standing in front of losing the user's
    // row to an UnexpectedRollbackException.
    @Test
    void shouldDeclareRequiresNewOnTheDetectionQuery() throws Exception {
        TransactionAttributeSource transactionAttributeSource =
                ((TransactionInterceptor) Arrays.stream(((Advised) gameSessionRepository).getAdvisors())
                                                 .map(Advisor::getAdvice)
                                                 .filter(TransactionInterceptor.class::isInstance)
                                                 .findFirst()
                                                 .orElseThrow()).getTransactionAttributeSource();

        TransactionAttribute transactionAttribute = transactionAttributeSource.getTransactionAttribute(
                GameSessionRepository.class.getMethod("getTop1ByGroupChatIdAndGameAndIdNotOrderByDurationAsc", Long.class, GameType.class, UUID.class),
                AopUtils.getTargetClass(gameSessionRepository));

        assertNotNull(transactionAttribute);
        assertAll(
                () -> assertEquals(TransactionDefinition.PROPAGATION_REQUIRES_NEW, transactionAttribute.getPropagationBehavior()),
                () -> assertTrue(transactionAttribute.isReadOnly())
        );
    }

    // T3 / AC-7, AC-18 — the reminder query's parked filter, over five clauses on ONE invocation: it excludes only the
    // players parked in THIS group (clause 2) on THAT game day (clause 3), with the end boundary exclusive (clause 4),
    // and it excludes nobody else (clauses 1 and 5). A method-level transaction is required because the query returns a
    // Stream and this class is deliberately not @Transactional.
    @Test
    @Transactional
    void shouldExcludeFromRemindersOnlyThePlayersParkedInThisGroupOnThatGameDay() {
        TelegramUser takingPart = seedUser(-900111L);
        TelegramUser parkedOverTheDay = seedUser(-900112L);
        TelegramUser closedOverTheDay = seedUser(-900113L);
        TelegramUser backOnTheDay = seedUser(-900114L);
        TelegramUser neverParked = seedUser(-900115L);

        TelegramGroup group = seedGroupWithMembers(T3_GROUP_CHAT_ID, takingPart, parkedOverTheDay, closedOverTheDay, backOnTheDay, neverParked);
        // clause 1 — an open period covering D
        park(group, parkedOverTheDay.getId(), T3_GAME_DAY, null);
        // clause 3 — a CLOSED period covering D. An open-only reading ("pp.endGameDay IS NULL") returns them and fails.
        park(group, closedOverTheDay.getId(), T3_GAME_DAY.minusDays(1), T3_GAME_DAY.plusDays(1));
        // clause 4 — un-parked ON D. This is the common path: auto-un-parking closes at today, so a player who came back
        // today holds exactly this shape, and an inclusive end boundary drops them from every reminder for the rest of
        // the day, in every group they belong to.
        park(group, backOnTheDay.getId(), T3_GAME_DAY.minusDays(1), T3_GAME_DAY);
        // clause 2 — the same parked user, in a second group where they hold no period at all
        seedGroupWithMembers(T3_OTHER_GROUP_CHAT_ID, parkedOverTheDay);

        Set<String> missing;
        try (Stream<MissingSessionUserProjection> projections = telegramUserRepository.findUsersWithMissingSessions(T3_GAME_DAY)) {
            missing = projections.map(projection -> projection.getChatId() + "/" + projection.getUserId())
                                  .collect(Collectors.toSet());
        }

        assertAll(
                () -> assertTrue(missing.contains(reminderKey(T3_GROUP_CHAT_ID, takingPart))),
                () -> assertFalse(missing.contains(reminderKey(T3_GROUP_CHAT_ID, parkedOverTheDay))),
                () -> assertTrue(missing.contains(reminderKey(T3_OTHER_GROUP_CHAT_ID, parkedOverTheDay))),
                () -> assertFalse(missing.contains(reminderKey(T3_GROUP_CHAT_ID, closedOverTheDay))),
                () -> assertTrue(missing.contains(reminderKey(T3_GROUP_CHAT_ID, backOnTheDay))),
                () -> assertTrue(missing.contains(reminderKey(T3_GROUP_CHAT_ID, neverParked)))
        );
    }

    // T4 / NF-1, NF-2 — the end-of-day sweep's site, over seven seeded groups on ONE invocation. The readings this
    // discriminates are: no filter at all (fails 4a, 4b, 4e), both counts filtered (fails 4b), the correlation dropped
    // (fails 4c), "is this player parked NOW?" (fails 4e and 4f) and an inclusive end boundary (fails 4g).
    @Test
    @Transactional
    void shouldSelectForRecalculationOnlyTheDaysWithSessionsOfPlayersTakingPartOnThatDay() throws Exception {
        // 4a — the regression itself: the day is published, a player is parked on it, and the leftover score rows are
        // what make the counts balance. 6 sessions, 4 score rows, B parked on D.
        TelegramUser a1 = seedUser(-900211L);
        TelegramUser sharedParked = seedUser(-900212L);
        TelegramUser c1 = seedUser(-900213L);
        TelegramGroup groupA = seedGroupWithMembers(T4_A_CHAT_ID, a1, sharedParked, c1);
        park(groupA, sharedParked.getId(), T4_GAME_DAY, null);
        scoreAll(groupA, twoSessions(groupA, a1));
        twoSessions(groupA, sharedParked);
        scoreAll(groupA, twoSessions(groupA, c1));

        // 4b — the asymmetry. Filtering the score count too gives 1 < 2 and republishes a closed day; the bound reading
        // gives 3 < 2 and leaves it alone.
        TelegramUser a2 = seedUser(-900221L);
        TelegramUser b2 = seedUser(-900222L);
        TelegramUser c2 = seedUser(-900223L);
        TelegramGroup groupB = seedGroupWithMembers(T4_B_CHAT_ID, a2, b2, c2);
        park(groupB, b2.getId(), T4_GAME_DAY, null);
        park(groupB, c2.getId(), T4_GAME_DAY, null);
        scoreAll(groupB, List.of(seedSession(groupB, a2, GameType.QUEENS, T4_GAME_DAY, 30)));
        seedSession(groupB, a2, GameType.ZIP, T4_GAME_DAY, 31);
        scoreAll(groupB, List.of(seedSession(groupB, b2, GameType.QUEENS, T4_GAME_DAY, 32)));
        scoreAll(groupB, List.of(seedSession(groupB, c2, GameType.QUEENS, T4_GAME_DAY, 33)));

        // 4c — the group correlation. Nobody is parked HERE; the single unscored session belongs to the user parked in
        // 4a's group. An uncorrelated NOT EXISTS reads "parked in any group" and stops selecting this group.
        TelegramGroup groupC = seedGroupWithMembers(T4_C_CHAT_ID, sharedParked);
        seedSession(groupC, sharedParked, GameType.QUEENS, T4_GAME_DAY, 40);

        // 4d — the over-filtering guard: no periods anywhere in this group.
        TelegramUser a4 = seedUser(-900241L);
        TelegramGroup groupD = seedGroupWithMembers(T4_D_CHAT_ID, a4);
        twoSessions(groupD, a4);

        // 4e — the un-parking direction. B was parked ON D and came back on D+1: asking "is B parked now?" springs the
        // session count back to 6 and republishes a closed day on every restart.
        TelegramUser a5 = seedUser(-900251L);
        TelegramUser b5 = seedUser(-900252L);
        TelegramUser c5 = seedUser(-900253L);
        TelegramGroup groupE = seedGroupWithMembers(T4_E_CHAT_ID, a5, b5, c5);
        park(groupE, b5.getId(), T4_GAME_DAY, T4_GAME_DAY.plusDays(1));
        scoreAll(groupE, twoSessions(groupE, a5));
        twoSessions(groupE, b5);
        scoreAll(groupE, twoSessions(groupE, c5));

        // 4f — the mirror. B was parked the morning AFTER D, so D must still be ranked over B. Asking "is B parked now?"
        // drops B's sessions from D and the day is never ranked at all.
        TelegramUser a6 = seedUser(-900261L);
        TelegramUser b6 = seedUser(-900262L);
        TelegramGroup groupF = seedGroupWithMembers(T4_F_CHAT_ID, a6, b6);
        park(groupF, b6.getId(), T4_GAME_DAY.plusDays(1), null);
        scoreAll(groupF, List.of(seedSession(groupF, a6, GameType.QUEENS, T4_GAME_DAY, 50),
                                  seedSession(groupF, a6, GameType.TANGO, T4_GAME_DAY, 51),
                                  seedSession(groupF, b6, GameType.QUEENS, T4_GAME_DAY, 52),
                                  seedSession(groupF, b6, GameType.TANGO, T4_GAME_DAY, 53)));
        seedSession(groupF, b6, GameType.ZIP, T4_GAME_DAY, 54);

        // 4g — the end-exclusive boundary, and the only case in the suite whose verdict changes on a one-character error
        // in the end comparison. X came back ON D, so X's two sessions still count for D and the day must be ranked.
        TelegramUser x7 = seedUser(-900271L);
        TelegramUser y7 = seedUser(-900272L);
        TelegramGroup groupG = seedGroupWithMembers(T4_G_CHAT_ID, x7, y7);
        park(groupG, x7.getId(), T4_GAME_DAY.minusDays(1), T4_GAME_DAY);
        List<GameSession> unscored = twoSessions(groupG, x7);
        scoreAll(groupG, twoSessions(groupG, y7));

        // Seed guard for 4c: without a period covering D in the OTHER group this case passes under every reading,
        // including the uncorrelated one, with no signal that it stopped discriminating.
        assertTrue(telegramGroupService.listPlayersParkedOn(T4_A_CHAT_ID, T4_GAME_DAY).contains(sharedParked.getId()));
        // Seed guard for 4g: it only discriminates while the score count equals the sessions of the OTHER player. One
        // score row short and the inclusive-end reading gives 1 < 2, selects the group, and the case goes green having
        // tested nothing.
        assertEquals(4, sessionsOn(T4_G_CHAT_ID, T4_GAME_DAY));
        assertEquals(2, scoresOn(T4_G_CHAT_ID, T4_GAME_DAY));
        assertEquals(Set.of(unscored.getFirst().getId(), unscored.getLast().getId()), unscoredSessionIds(T4_G_CHAT_ID, T4_GAME_DAY));

        Set<Long> selected;
        try (Stream<TelegramGroup> groups = telegramGroupRepository.findGroupsWithMissingScores(T4_GAME_DAY)) {
            selected = groups.map(TelegramGroup::getChatId).collect(Collectors.toSet());
        }

        assertAll(
                () -> assertFalse(selected.contains(T4_A_CHAT_ID)),
                () -> assertFalse(selected.contains(T4_B_CHAT_ID)),
                () -> assertTrue(selected.contains(T4_C_CHAT_ID)),
                () -> assertTrue(selected.contains(T4_D_CHAT_ID)),
                () -> assertFalse(selected.contains(T4_E_CHAT_ID)),
                () -> assertTrue(selected.contains(T4_F_CHAT_ID)),
                () -> assertTrue(selected.contains(T4_G_CHAT_ID))
        );
    }

    // T5 / AC-5, AC-16 — the toggle publishes exactly one event carrying the participating count AFTER the change, and
    // the period actually reaches the database. Deliberately NOT @Transactional: the toggle must really commit, because
    // reading a period back after its writing transaction committed is the only automated check on the association's
    // PERSIST/MERGE cascade. Get that annotation wrong and the toggle announces a park that was never written.
    @Test
    void shouldPublishTheParticipatingCountAndPersistThePeriodWhenParkingAPlayer() throws Exception {
        TelegramUser parked = seedUser(-900311L);
        TelegramUser remaining = seedUser(-900312L);
        seedGroupWithMembers(T5_CHAT_ID, parked, remaining);

        telegramGroupService.togglePlayerParticipation(T5_CHAT_ID, parked.getId());

        List<PlayerParticipationChangedEvent> events = participationEvents(T5_CHAT_ID);
        assertEquals(1, events.size());
        assertAll(
                () -> assertTrue(events.getFirst().isParked()),
                () -> assertEquals(parked.getId(), events.getFirst().getPlayer().id()),
                () -> assertEquals(1, events.getFirst().getPlayersTakingPart()),
                () -> assertTrue(telegramGroupService.listPlayersParkedOn(T5_CHAT_ID, LinkedinTimeUtils.todayGameDay()).contains(parked.getId()))
        );
    }

    // T5 / AC-16 — parking the last player leaves nobody taking part, and the count on the event is what the last-player
    // warning is rendered from. Its own group id and its own users: group state is never reset between methods, and the
    // toggle takes no desired state, so a group carrying a period from an earlier method would un-park instead of park.
    @Test
    void shouldPublishZeroPlayersTakingPartWhenParkingTheLastOne() throws Exception {
        TelegramUser first = seedUser(-900321L);
        TelegramUser last = seedUser(-900322L);
        seedGroupWithMembers(T5_LAST_CHAT_ID, first, last);

        telegramGroupService.togglePlayerParticipation(T5_LAST_CHAT_ID, first.getId());
        telegramGroupService.togglePlayerParticipation(T5_LAST_CHAT_ID, last.getId());

        List<PlayerParticipationChangedEvent> events = participationEvents(T5_LAST_CHAT_ID);
        assertEquals(2, events.size());
        assertAll(
                () -> assertEquals(1, events.getFirst().getPlayersTakingPart()),
                () -> assertEquals(0, events.getLast().getPlayersTakingPart()),
                () -> assertTrue(events.getLast().isParked())
        );
    }

    private TelegramUser seedUser(final Long id) {
        TelegramUser telegramUser = new TelegramUser();
        telegramUser.setId(id);
        telegramUser.setUserName("user" + Math.abs(id));
        telegramUser.setFirstName("User");
        telegramUser.setLastName(String.valueOf(Math.abs(id)));
        return telegramUserRepository.save(telegramUser);
    }

    private TelegramGroup seedGroupWithMembers(final Long chatId, final TelegramUser... members) {
        TelegramGroup telegramGroup = new TelegramGroup(chatId, "Group " + chatId);
        telegramGroup.setTrackedGames(EnumSet.of(GAME));
        telegramGroup.getMembers().addAll(List.of(members));
        return telegramGroupRepository.save(telegramGroup);
    }

    private void park(final TelegramGroup telegramGroup, final Long userId, final LocalDate startGameDay, final LocalDate endGameDay) {
        ParkedPeriod parkedPeriod = new ParkedPeriod(telegramGroup, userId, startGameDay);
        parkedPeriod.setEndGameDay(endGameDay);
        telegramGroup.getParkedPeriods().add(parkedPeriod);
        telegramGroupRepository.save(telegramGroup);
    }

    private List<GameSession> twoSessions(final TelegramGroup telegramGroup, final TelegramUser telegramUser) {
        return List.of(seedSession(telegramGroup, telegramUser, GameType.QUEENS, T4_GAME_DAY, 60),
                        seedSession(telegramGroup, telegramUser, GameType.ZIP, T4_GAME_DAY, 61));
    }

    private GameSession seedSession(final TelegramGroup telegramGroup, final TelegramUser telegramUser, final GameType game, final LocalDate gameDay, final int seconds) {
        GameSession gameSession = new GameSession();
        gameSession.setGroup(telegramGroup);
        gameSession.setUser(telegramUser);
        gameSession.setGame(game);
        gameSession.setGameDay(gameDay);
        gameSession.setDuration(Duration.ofSeconds(seconds));
        gameSession.setRegisteredAt(Instant.now());
        return gameSessionRepository.save(gameSession);
    }

    private void scoreAll(final TelegramGroup telegramGroup, final List<GameSession> sessions) {
        sessions.forEach(gameSession -> {
            DailyGameScore dailyGameScore = new DailyGameScore();
            dailyGameScore.setGroup(telegramGroup);
            dailyGameScore.setUser(gameSession.getUser());
            dailyGameScore.setGame(gameSession.getGame());
            dailyGameScore.setGameDay(gameSession.getGameDay());
            dailyGameScore.setGameSession(gameSession);
            dailyGameScore.setPosition(1);
            dailyGameScore.setPoints(1);
            dailyScoreRepository.save(dailyGameScore);
        });
    }

    private long sessionsOn(final Long chatId, final LocalDate gameDay) {
        return gameSessionRepository.findAll().stream()
                                     .filter(gameSession -> chatId.equals(gameSession.getGroup().getChatId()) && gameDay.equals(gameSession.getGameDay()))
                                     .count();
    }

    private long scoresOn(final Long chatId, final LocalDate gameDay) {
        return scoredSessionIds(chatId, gameDay).size();
    }

    private Set<UUID> scoredSessionIds(final Long chatId, final LocalDate gameDay) {
        return dailyScoreRepository.findAll().stream()
                                    .filter(score -> chatId.equals(score.getGroup().getChatId()) && gameDay.equals(score.getGameDay()))
                                    .map(score -> score.getGameSession().getId())
                                    .collect(Collectors.toSet());
    }

    private Set<UUID> unscoredSessionIds(final Long chatId, final LocalDate gameDay) {
        Set<UUID> scored = scoredSessionIds(chatId, gameDay);
        return gameSessionRepository.findAll().stream()
                                     .filter(gameSession -> chatId.equals(gameSession.getGroup().getChatId()) && gameDay.equals(gameSession.getGameDay()))
                                     .map(GameSession::getId)
                                     .filter(id -> !scored.contains(id))
                                     .collect(Collectors.toSet());
    }

    private String reminderKey(final Long chatId, final TelegramUser telegramUser) {
        return chatId + "/" + telegramUser.getId();
    }

    private List<PlayerParticipationChangedEvent> participationEvents(final Long chatId) {
        return applicationEvents.stream(PlayerParticipationChangedEvent.class)
                                 .filter(event -> chatId.equals(event.getChatId()))
                                 .toList();
    }

    private void submit(final Long chatId, final UserInfo userInfo, final int seconds, final LocalDate gameDay, final boolean allowOverride)
            throws SessionAlreadyRegisteredException, GroupNotFoundException {
        gameSessionService.recordGameSession(new ChatInfo(chatId, "Group " + chatId, true), userInfo, new GameDuration(GAME, Duration.ofSeconds(seconds)),
                                              gameDay, Instant.now(), allowOverride);
    }

    private List<GameRecordEstablishedEvent> recordEvents() {
        return applicationEvents.stream(GameRecordEstablishedEvent.class).toList();
    }

    private List<GameRecordEstablishedEvent> recordEvents(final Long chatId) {
        return applicationEvents.stream(GameRecordEstablishedEvent.class)
                                 .filter(event -> chatId.equals(event.getChatInfo().chatId()))
                                 .toList();
    }

    private void seedGroup(final Long chatId, final Set<GameType> trackedGames) {
        TelegramGroup telegramGroup = new TelegramGroup(chatId, "Group " + chatId);
        telegramGroup.setTrackedGames(trackedGames);
        telegramGroupRepository.save(telegramGroup);
    }

}
