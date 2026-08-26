package dev.rubasace.linkedin.games.ldrbot.chat;

import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.session.GameInfo;
import dev.rubasace.linkedin.games.ldrbot.session.GameRecordEstablishedEvent;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class RecordNotificationTest {

    private static final Long CHAT_ID = 4242L;
    private static final ChatInfo CHAT_INFO = new ChatInfo(CHAT_ID, "The Group", true);
    private static final GameInfo GAME_INFO = new GameInfo("Queens", "👑");
    private static final UserInfo USER_WITH_USERNAME = new UserInfo(7L, "ada", "Ada", "Lovelace");
    private static final UserInfo USER_WITHOUT_USERNAME = new UserInfo(7L, null, "Ada", "Lovelace");
    private static final Duration NEW_RECORD = Duration.ofSeconds(90);
    private static final Duration BEST_OTHER = Duration.ofSeconds(125);

    private final CustomTelegramClient customTelegramClient = mock(CustomTelegramClient.class);
    private final RankingMessageFactory rankingMessageFactory = mock(RankingMessageFactory.class);
    private final NotificationService notificationService = new NotificationService(customTelegramClient, rankingMessageFactory);

    // AC1, AC3, AC8, AC9 — record-broken wording carries the new time and the best OTHER time, both as mm:ss.
    @Test
    void shouldIncludeBestOtherTimeWhenRecordBroken() {
        notificationService.handleRecordEstablished(recordEvent(USER_WITH_USERNAME, NEW_RECORD, BEST_OTHER));

        String message = capturedMessage();
        assertAll(
                () -> assertTrue(message.contains("beat the group's best"), message),
                () -> assertTrue(message.contains("ahead of the next best time of"), message),
                () -> assertTrue(message.contains("01:30"), message),
                () -> assertTrue(message.contains("02:05"), message),
                () -> assertFalse(message.contains("PT"), message),
                () -> assertTrue(message.contains("👑"), message),
                () -> assertTrue(message.contains("Queens"), message)
        );
    }

    // AC2, AC8, AC9 — no other session: first-record wording, and no "next best time" clause at all.
    @Test
    void shouldUseFirstRecordWordingWhenNoOtherSession() {
        notificationService.handleRecordEstablished(recordEvent(USER_WITH_USERNAME, NEW_RECORD, null));

        String message = capturedMessage();
        assertAll(
                () -> assertTrue(message.contains("set the group's first"), message),
                () -> assertFalse(message.contains("ahead of the next best time of"), message),
                () -> assertTrue(message.contains("01:30"), message),
                () -> assertFalse(message.contains("PT"), message),
                () -> assertTrue(message.contains("👑"), message),
                () -> assertTrue(message.contains("Queens"), message)
        );
    }

    // AC6 — one message, to the event's own chat and to no other.
    @Test
    void shouldSendToTheGroupChat() {
        notificationService.handleRecordEstablished(recordEvent(USER_WITH_USERNAME, NEW_RECORD, BEST_OTHER));

        verify(customTelegramClient).sendMessage(any(), eq(CHAT_ID));
        verifyNoMoreInteractions(customTelegramClient);
    }

    // AC7 — a user with no @username is mentioned through the HTML tg:// anchor.
    @Test
    void shouldMentionUserWithoutUsername() {
        notificationService.handleRecordEstablished(recordEvent(USER_WITHOUT_USERNAME, NEW_RECORD, BEST_OTHER));

        String message = capturedMessage();
        assertTrue(message.contains("<a href=\"tg://user?id=7\">Ada Lovelace</a>"), message);
    }

    // AC7 — a user with a @username is mentioned by handle.
    @Test
    void shouldMentionUserWithUsername() {
        notificationService.handleRecordEstablished(recordEvent(USER_WITH_USERNAME, NEW_RECORD, null));

        String message = capturedMessage();
        assertTrue(message.contains("@ada"), message);
    }

    private GameRecordEstablishedEvent recordEvent(final UserInfo userInfo, final Duration duration, final Duration bestOtherDuration) {
        return new GameRecordEstablishedEvent(this, CHAT_INFO, userInfo, GAME_INFO, duration, bestOtherDuration);
    }

    private String capturedMessage() {
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(customTelegramClient).sendMessage(messageCaptor.capture(), eq(CHAT_ID));
        return messageCaptor.getValue();
    }

}
