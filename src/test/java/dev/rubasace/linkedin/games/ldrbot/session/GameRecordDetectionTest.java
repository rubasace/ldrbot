package dev.rubasace.linkedin.games.ldrbot.session;

import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.group.GroupNotFoundException;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroup;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupRepository;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private static final GameType GAME = GameType.QUEENS;
    private static final LocalDate GAME_DAY = LocalDate.of(2025, 3, 10);
    private static final LocalDate OLDER_GAME_DAY = LocalDate.of(2025, 2, 3);

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
