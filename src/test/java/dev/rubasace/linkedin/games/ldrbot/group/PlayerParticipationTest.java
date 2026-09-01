package dev.rubasace.linkedin.games.ldrbot.group;

import dev.rubasace.linkedin.games.ldrbot.session.GameTypeAdapter;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUser;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUserAdapter;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUserService;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import dev.rubasace.linkedin.games.ldrbot.util.LinkedinTimeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T2 — the day-scoped parked predicate and the three mutators that write periods, in plain JUnit against the entity and
 * the service. No Spring context: everything here is decided in Java, and the two JPQL copies of the same predicate are
 * covered by {@code GameRecordDetectionTest}.
 * <p>
 * Covers AC-4, AC-11, AC-12, AC-13, AC-14, AC-15 and AC-19.
 */
class PlayerParticipationTest {

    private static final Long CHAT_ID = -800001L;
    private static final Long PLAYER_ID = -800101L;
    private static final Long STRANGER_ID = -800102L;
    private static final LocalDate TODAY = LinkedinTimeUtils.todayGameDay();

    private final TelegramGroupRepository telegramGroupRepository = mock(TelegramGroupRepository.class);
    private final TelegramUserService telegramUserService = mock(TelegramUserService.class);
    private final ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
    private final TelegramGroupService telegramGroupService =
            new TelegramGroupService(telegramGroupRepository, telegramUserService, applicationEventPublisher, new GameTypeAdapter(), new TelegramUserAdapter());

    private TelegramGroup telegramGroup;
    private TelegramUser player;

    @BeforeEach
    void setUp() {
        player = new TelegramUser();
        player.setId(PLAYER_ID);
        player.setUserName("player");
        telegramGroup = new TelegramGroup(CHAT_ID, "Test Group");
        telegramGroup.getMembers().add(player);
        when(telegramGroupRepository.findById(CHAT_ID)).thenReturn(Optional.of(telegramGroup));
        when(telegramUserService.find(PLAYER_ID)).thenReturn(Optional.of(player));
        when(telegramUserService.find(any(UserInfo.class))).thenReturn(Optional.of(player));
    }

    // Cases a and b — start is inclusive, and yesterday is untouched by a parking made today.
    @Test
    void shouldCoverTheStartDayAndNothingBeforeIt() {
        park(TODAY, null);

        assertAll(
                () -> assertTrue(telegramGroup.isParkedOn(PLAYER_ID, TODAY)),
                () -> assertFalse(telegramGroup.isParkedOn(PLAYER_ID, TODAY.minusDays(1)))
        );
    }

    // Cases c, d and e — a closed period covers its start and the days between, and NOT its end. Case d is the boundary
    // an off-by-one lands on: the un-parking day itself counts the player again.
    @Test
    void shouldCoverAClosedPeriodStartInclusiveAndEndExclusive() {
        LocalDate monday = LocalDate.of(2025, 4, 7);
        park(monday, monday.plusDays(2));

        assertAll(
                () -> assertTrue(telegramGroup.isParkedOn(PLAYER_ID, monday)),
                () -> assertTrue(telegramGroup.isParkedOn(PLAYER_ID, monday.plusDays(1))),
                () -> assertFalse(telegramGroup.isParkedOn(PLAYER_ID, monday.plusDays(2))),
                () -> assertFalse(telegramGroup.isParkedOn(PLAYER_ID, monday.minusDays(1)))
        );
    }

    // Case f — parking and bringing a player back within one game day leaves an empty period, which covers no day at
    // all. This is what keeps that day counting the player, and it is what makes AC-11 true.
    @Test
    void shouldNotCoverAnyDayWithAnEmptyPeriod() {
        park(TODAY, TODAY);

        assertFalse(telegramGroup.isParkedOn(PLAYER_ID, TODAY));
    }

    // Case g — absence is the default. No period means the player was never parked on any day, which is NF-1 by
    // construction: nothing is back-filled and no default row exists.
    @Test
    void shouldNeverBeParkedWithoutAnyPeriod() {
        assertAll(
                () -> assertFalse(telegramGroup.isParkedOn(PLAYER_ID, TODAY)),
                () -> assertFalse(telegramGroup.isParkedOn(PLAYER_ID, TODAY.minusDays(400))),
                () -> assertTrue(telegramGroup.getParticipatingMembers(TODAY).contains(player))
        );
    }

    // Case h — the gap between two periods is not covered by either of them.
    @Test
    void shouldNotCoverTheGapBetweenTwoPeriods() {
        LocalDate monday = LocalDate.of(2025, 4, 7);
        park(monday, monday.plusDays(2));
        park(monday.plusDays(4), null);

        assertAll(
                () -> assertFalse(telegramGroup.isParkedOn(PLAYER_ID, monday.plusDays(3))),
                () -> assertTrue(telegramGroup.isParkedOn(PLAYER_ID, monday.plusDays(4)))
        );
    }

    // Case i / AC-4, AC-19 — the toggle carries no desired state: it asks the predicate at today and does the opposite,
    // however many times it is tapped in one day. Four taps leave TWO rows, both empty, and delete nothing.
    @Test
    void shouldFlipParticipationOnEveryToggleAndKeepTheHistory() throws Exception {
        boolean afterFirst = toggleAndRead();
        boolean afterSecond = toggleAndRead();
        boolean afterThird = toggleAndRead();
        boolean afterFourth = toggleAndRead();

        ArgumentCaptor<ApplicationEvent> published = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(applicationEventPublisher, times(4)).publishEvent(published.capture());
        List<Boolean> announcedStates = published.getAllValues().stream()
                                                  .map(event -> ((PlayerParticipationChangedEvent) event).isParked())
                                                  .toList();

        assertAll(
                () -> assertTrue(afterFirst),
                () -> assertFalse(afterSecond),
                () -> assertTrue(afterThird),
                () -> assertFalse(afterFourth),
                () -> assertEquals(List.of(true, false, true, false), announcedStates),
                () -> assertEquals(2, telegramGroup.getParkedPeriods().size()),
                () -> assertTrue(telegramGroup.getParkedPeriods().stream()
                                               .allMatch(period -> TODAY.equals(period.getStartGameDay()) && TODAY.equals(period.getEndGameDay())))
        );
    }

    // §1.5 clause 5 — the player left the group between the list being drawn and the button being tapped. No period is
    // opened and nothing is announced, so no orphan row and no message about a change that never happened.
    @Test
    void shouldDoNothingWhenTogglingSomebodyWhoIsNotAMember() throws Exception {
        telegramGroupService.togglePlayerParticipation(CHAT_ID, STRANGER_ID);

        assertAll(
                () -> assertTrue(telegramGroup.getParkedPeriods().isEmpty()),
                () -> verify(applicationEventPublisher, never()).publishEvent(any(PlayerParticipationChangedEvent.class))
        );
    }

    // Case j / AC-13 — a result for a day strictly before the parking does not bring the player back and announces
    // nothing. The result is still recorded; it simply does not score for a day the player was parked on.
    @Test
    void shouldNotUnparkOnAResultForADayBeforeTheParking() throws Exception {
        LocalDate start = TODAY.minusDays(2);
        park(start, null);

        telegramGroupService.unparkPlayerForResult(CHAT_ID, PLAYER_ID, start.minusDays(1));

        assertAll(
                () -> assertTrue(telegramGroup.isParkedOn(PLAYER_ID, TODAY)),
                () -> assertNull(onlyPeriod().getEndGameDay()),
                () -> verify(applicationEventPublisher, never()).publishEvent(any(PlayerParticipationChangedEvent.class))
        );
    }

    // Case k / AC-12, AC-14 — a result on or after the parking day brings the player back and announces it once. The
    // period is closed at TODAY and never at the result's game day: closing at the result day would write a boundary
    // into the past and silently change that day's participation set.
    @Test
    void shouldUnparkOnAResultForTheParkingDayAndCloseAtToday() throws Exception {
        LocalDate start = TODAY.minusDays(2);
        park(start, null);

        telegramGroupService.unparkPlayerForResult(CHAT_ID, PLAYER_ID, start);

        assertAll(
                () -> assertFalse(telegramGroup.isParkedOn(PLAYER_ID, TODAY)),
                () -> assertEquals(TODAY, onlyPeriod().getEndGameDay()),
                () -> assertNotEquals(start, onlyPeriod().getEndGameDay()),
                () -> assertTrue(telegramGroup.isParkedOn(PLAYER_ID, start)),
                () -> verify(applicationEventPublisher, times(1)).publishEvent(any(PlayerParticipationChangedEvent.class))
        );
    }

    // Case l — a result landing inside an ALREADY-CLOSED period changes nothing: the period is not re-opened, not split
    // around the result's day and not shortened, and nothing is announced. Any of those writes a boundary for a day
    // other than today.
    @Test
    void shouldLeaveAnAlreadyClosedPeriodAloneWhenAResultLandsInsideIt() throws Exception {
        LocalDate monday = TODAY.minusDays(10);
        LocalDate wednesday = monday.plusDays(2);
        park(monday, wednesday);

        telegramGroupService.unparkPlayerForResult(CHAT_ID, PLAYER_ID, monday.plusDays(1));

        assertAll(
                () -> assertEquals(1, telegramGroup.getParkedPeriods().size()),
                () -> assertEquals(monday, onlyPeriod().getStartGameDay()),
                () -> assertEquals(wednesday, onlyPeriod().getEndGameDay()),
                () -> assertTrue(telegramGroup.isParkedOn(PLAYER_ID, monday.plusDays(1))),
                () -> verify(applicationEventPublisher, never()).publishEvent(any(PlayerParticipationChangedEvent.class))
        );
    }

    // Case m / AC-15 — leaving the group CLOSES the open period rather than purging the history. A purge would put the
    // player back into the participation set of every past day they were parked on as soon as they rejoined.
    @Test
    void shouldCloseNotDeleteThePeriodWhenAPlayerLeavesTheGroup() throws Exception {
        LocalDate start = TODAY.minusDays(3);
        park(start, null);

        telegramGroupService.removeUserFromGroup(new ChatInfo(CHAT_ID, "Test Group", true), new UserInfo(PLAYER_ID, "player", "Player", "One"));

        assertAll(
                () -> assertEquals(1, telegramGroup.getParkedPeriods().size()),
                () -> assertEquals(TODAY, onlyPeriod().getEndGameDay()),
                () -> assertTrue(telegramGroup.isParkedOn(PLAYER_ID, start)),
                () -> assertFalse(telegramGroup.isParkedOn(PLAYER_ID, TODAY)),
                () -> assertTrue(telegramGroup.getMembers().isEmpty())
        );
    }

    private boolean toggleAndRead() throws GroupNotFoundException {
        telegramGroupService.togglePlayerParticipation(CHAT_ID, PLAYER_ID);
        return telegramGroup.isParkedOn(PLAYER_ID, TODAY);
    }

    private void park(final LocalDate startGameDay, final LocalDate endGameDay) {
        ParkedPeriod parkedPeriod = new ParkedPeriod(telegramGroup, PLAYER_ID, startGameDay);
        parkedPeriod.setEndGameDay(endGameDay);
        telegramGroup.getParkedPeriods().add(parkedPeriod);
    }

    private ParkedPeriod onlyPeriod() {
        assertEquals(1, telegramGroup.getParkedPeriods().size());
        return telegramGroup.getParkedPeriods().iterator().next();
    }
}
