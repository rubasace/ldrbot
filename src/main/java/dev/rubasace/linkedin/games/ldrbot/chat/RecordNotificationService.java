package dev.rubasace.linkedin.games.ldrbot.chat;

import dev.rubasace.linkedin.games.ldrbot.configuration.ExecutorsConfiguration;
import dev.rubasace.linkedin.games.ldrbot.session.GameSession;
import dev.rubasace.linkedin.games.ldrbot.session.GameSessionRegistrationEvent;
import dev.rubasace.linkedin.games.ldrbot.session.GameSessionRepository;
import dev.rubasace.linkedin.games.ldrbot.util.FormatUtils;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.util.List;

@Service
public class RecordNotificationService {

    private static final String NEW_RECORD_MESSAGE = "🏆 New record! %s completed %s in %s, beating the previous record of %s!";
    private static final String FIRST_RECORD_MESSAGE = "🏆 New record! %s completed %s in %s!";

    private final GameSessionRepository gameSessionRepository;
    private final CustomTelegramClient customTelegramClient;

    RecordNotificationService(final GameSessionRepository gameSessionRepository, final CustomTelegramClient customTelegramClient) {
        this.gameSessionRepository = gameSessionRepository;
        this.customTelegramClient = customTelegramClient;
    }

    @Order(NotificationService.USER_INTERACTION_NOTIFICATION_ORDER)
    @Async(ExecutorsConfiguration.NOTIFICATION_LISTENER_EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleRecordNotification(final GameSessionRegistrationEvent event) {
        List<GameSession> top2Sessions = gameSessionRepository.findTop2ByGroupChatIdAndGameOrderByDurationAsc(
                event.getChatId(),
                event.getGameType()
        );

        if (top2Sessions.isEmpty()) {
            return;
        }

        GameSession bestSession = top2Sessions.get(0);
        Duration bestDuration = bestSession.getDuration();

        if (!bestDuration.equals(event.getDuration())) {
            return;
        }

        String userMention = FormatUtils.formatUserMention(event.getUserInfo());
        String gameName = event.getGameInfo().name();
        String formattedDuration = FormatUtils.formatDuration(bestDuration);

        String message;
        if (top2Sessions.size() == 1) {
            message = FIRST_RECORD_MESSAGE.formatted(userMention, gameName, formattedDuration);
        } else {
            Duration previousRecord = top2Sessions.get(1).getDuration();
            String formattedPreviousRecord = FormatUtils.formatDuration(previousRecord);
            message = NEW_RECORD_MESSAGE.formatted(userMention, gameName, formattedDuration, formattedPreviousRecord);
        }

        customTelegramClient.sendMessage(message, event.getChatInfo().chatId());
    }
}
