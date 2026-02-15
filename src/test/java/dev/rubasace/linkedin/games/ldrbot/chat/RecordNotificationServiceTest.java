package dev.rubasace.linkedin.games.ldrbot.chat;

import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.session.GameInfo;
import dev.rubasace.linkedin.games.ldrbot.session.GameSession;
import dev.rubasace.linkedin.games.ldrbot.session.GameSessionRegistrationEvent;
import dev.rubasace.linkedin.games.ldrbot.session.GameSessionRepository;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordNotificationServiceTest {

    @Mock
    private GameSessionRepository gameSessionRepository;

    @Mock
    private CustomTelegramClient customTelegramClient;

    @InjectMocks
    private RecordNotificationService recordNotificationService;

    @Test
    void shouldNotNotifyWhenNotARecord() {
        GameSessionRegistrationEvent event = createEvent(Duration.ofMinutes(2));
        GameSession bestSession = createGameSession(Duration.ofMinutes(1));

        when(gameSessionRepository.findTop2ByGroupChatIdAndGameOrderByDurationAsc(anyLong(), eq(GameType.QUEENS)))
                .thenReturn(List.of(bestSession));

        recordNotificationService.handleRecordNotification(event);

        verify(customTelegramClient, never()).sendMessage(org.mockito.ArgumentMatchers.anyString(), anyLong());
    }

    @Test
    void shouldNotifyWhenFirstRecord() {
        Duration recordDuration = Duration.ofMinutes(1);
        GameSessionRegistrationEvent event = createEvent(recordDuration);
        GameSession bestSession = createGameSession(recordDuration);

        when(gameSessionRepository.findTop2ByGroupChatIdAndGameOrderByDurationAsc(anyLong(), eq(GameType.QUEENS)))
                .thenReturn(List.of(bestSession));

        recordNotificationService.handleRecordNotification(event);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(customTelegramClient).sendMessage(messageCaptor.capture(), eq(123L));

        String message = messageCaptor.getValue();
        assertTrue(message.contains("🏆"));
        assertTrue(message.contains("New record"));
        assertTrue(message.contains("@testuser"));
        assertTrue(message.contains("Queens"));
        assertTrue(message.contains("01:00"));
        assertFalse(message.contains("beating"));
    }

    @Test
    void shouldNotifyWhenBrokenRecord() {
        Duration recordDuration = Duration.ofMinutes(1);
        Duration previousRecord = Duration.ofMinutes(2);
        GameSessionRegistrationEvent event = createEvent(recordDuration);
        GameSession bestSession = createGameSession(recordDuration);
        GameSession secondBestSession = createGameSession(previousRecord);

        when(gameSessionRepository.findTop2ByGroupChatIdAndGameOrderByDurationAsc(anyLong(), eq(GameType.QUEENS)))
                .thenReturn(List.of(bestSession, secondBestSession));

        recordNotificationService.handleRecordNotification(event);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(customTelegramClient).sendMessage(messageCaptor.capture(), eq(123L));

        String message = messageCaptor.getValue();
        assertTrue(message.contains("🏆"));
        assertTrue(message.contains("New record"));
        assertTrue(message.contains("@testuser"));
        assertTrue(message.contains("Queens"));
        assertTrue(message.contains("01:00"));
        assertTrue(message.contains("beating"));
        assertTrue(message.contains("02:00"));
    }

    @Test
    void shouldHandleUserWithoutUsername() {
        Duration recordDuration = Duration.ofMinutes(1);
        UserInfo userWithoutUsername = new UserInfo(456L, null, "John", "Doe");
        GameSessionRegistrationEvent event = createEventWithUser(recordDuration, userWithoutUsername);
        GameSession bestSession = createGameSession(recordDuration);

        when(gameSessionRepository.findTop2ByGroupChatIdAndGameOrderByDurationAsc(anyLong(), eq(GameType.QUEENS)))
                .thenReturn(List.of(bestSession));

        recordNotificationService.handleRecordNotification(event);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(customTelegramClient).sendMessage(messageCaptor.capture(), eq(123L));

        String message = messageCaptor.getValue();
        assertTrue(message.contains("<a href=\"tg://user?id=456\">"));
        assertTrue(message.contains("John Doe"));
    }

    @Test
    void shouldNotNotifyWhenNoSessions() {
        GameSessionRegistrationEvent event = createEvent(Duration.ofMinutes(1));

        when(gameSessionRepository.findTop2ByGroupChatIdAndGameOrderByDurationAsc(anyLong(), eq(GameType.QUEENS)))
                .thenReturn(Collections.emptyList());

        recordNotificationService.handleRecordNotification(event);

        verify(customTelegramClient, never()).sendMessage(org.mockito.ArgumentMatchers.anyString(), anyLong());
    }

    private GameSessionRegistrationEvent createEvent(final Duration duration) {
        UserInfo userInfo = new UserInfo(456L, "testuser", "Test", "User");
        return createEventWithUser(duration, userInfo);
    }

    private GameSessionRegistrationEvent createEventWithUser(final Duration duration, final UserInfo userInfo) {
        ChatInfo chatInfo = new ChatInfo(123L, "Test Group", true);
        GameInfo gameInfo = new GameInfo("Queens", "👑");
        return new GameSessionRegistrationEvent(
                this,
                chatInfo,
                userInfo,
                gameInfo,
                GameType.QUEENS,
                duration,
                LocalDate.now(),
                123L
        );
    }

    private GameSession createGameSession(final Duration duration) {
        GameSession session = new GameSession();
        session.setDuration(duration);
        return session;
    }
}
