package dev.rubasace.linkedin.games.ldrbot.chat;

import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.session.*;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecordNotificationServiceTest {

    @Mock
    private CustomTelegramClient customTelegramClient;

    @Mock
    private GameSessionRepository gameSessionRepository;

    private RecordNotificationService service;

    private static final Long CHAT_ID = 123L;
    private static final GameType GAME = GameType.QUEENS;
    private static final ChatInfo CHAT_INFO = new ChatInfo(CHAT_ID, "Test Group", true);
    private static final UserInfo USER_INFO = new UserInfo(1L, "testuser", "Test", "User");
    private static final GameInfo GAME_INFO = new GameInfo("Queens", "👑");

    @BeforeEach
    void setUp() {
        service = new RecordNotificationService(customTelegramClient, gameSessionRepository);
    }

    private GameSessionRegistrationEvent createEvent(Duration duration) {
        return new GameSessionRegistrationEvent(this, CHAT_INFO, USER_INFO, GAME_INFO, GAME, duration, LocalDate.now(), CHAT_ID);
    }

    @Test
    void shouldNotNotifyWhenSubmittedDurationIsNotTheBest() {
        Duration submitted = Duration.ofSeconds(120);
        Duration best = Duration.ofSeconds(60);
        when(gameSessionRepository.findBestDuration(CHAT_ID, GAME)).thenReturn(Optional.of(best));

        service.handleSessionRegistration(createEvent(submitted));

        verifyNoInteractions(customTelegramClient);
    }

    @Test
    void shouldNotNotifyWhenNoBestDurationFound() {
        when(gameSessionRepository.findBestDuration(CHAT_ID, GAME)).thenReturn(Optional.empty());

        service.handleSessionRegistration(createEvent(Duration.ofSeconds(60)));

        verifyNoInteractions(customTelegramClient);
    }

    @Test
    void shouldNotifyFirstRecordEver() {
        Duration submitted = Duration.ofSeconds(90);
        when(gameSessionRepository.findBestDuration(CHAT_ID, GAME)).thenReturn(Optional.of(submitted));
        when(gameSessionRepository.findSecondBestDuration(CHAT_ID, GAME, submitted)).thenReturn(Optional.empty());

        service.handleSessionRegistration(createEvent(submitted));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(customTelegramClient).sendMessage(messageCaptor.capture(), eq(CHAT_ID));
        String message = messageCaptor.getValue();
        assertTrue(message.contains("New Queens record!"));
        assertTrue(message.contains("@testuser"));
        assertTrue(message.contains("01:30"));
        assertFalse(message.contains("previous record"));
    }

    @Test
    void shouldNotifyRecordBrokenWithPreviousBest() {
        Duration submitted = Duration.ofSeconds(45);
        Duration previous = Duration.ofSeconds(90);
        when(gameSessionRepository.findBestDuration(CHAT_ID, GAME)).thenReturn(Optional.of(submitted));
        when(gameSessionRepository.findSecondBestDuration(CHAT_ID, GAME, submitted)).thenReturn(Optional.of(previous));

        service.handleSessionRegistration(createEvent(submitted));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(customTelegramClient).sendMessage(messageCaptor.capture(), eq(CHAT_ID));
        String message = messageCaptor.getValue();
        assertTrue(message.contains("New Queens record!"));
        assertTrue(message.contains("@testuser"));
        assertTrue(message.contains("00:45"));
        assertTrue(message.contains("previous record: 01:30"));
    }

    @Test
    void shouldFormatUserWithoutUsernameAsMention() {
        UserInfo noUsername = new UserInfo(42L, null, "John", "Doe");
        Duration submitted = Duration.ofSeconds(30);
        GameSessionRegistrationEvent event = new GameSessionRegistrationEvent(this, CHAT_INFO, noUsername, GAME_INFO, GAME, submitted, LocalDate.now(), CHAT_ID);
        when(gameSessionRepository.findBestDuration(CHAT_ID, GAME)).thenReturn(Optional.of(submitted));
        when(gameSessionRepository.findSecondBestDuration(CHAT_ID, GAME, submitted)).thenReturn(Optional.empty());

        service.handleSessionRegistration(event);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(customTelegramClient).sendMessage(messageCaptor.capture(), eq(CHAT_ID));
        String message = messageCaptor.getValue();
        assertTrue(message.contains("tg://user?id=42"));
        assertTrue(message.contains("John Doe"));
    }
}
