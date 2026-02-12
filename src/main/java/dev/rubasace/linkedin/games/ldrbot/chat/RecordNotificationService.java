package dev.rubasace.linkedin.games.ldrbot.chat;

import dev.rubasace.linkedin.games.ldrbot.configuration.ExecutorsConfiguration;
import dev.rubasace.linkedin.games.ldrbot.session.GameSessionRegistrationEvent;
import dev.rubasace.linkedin.games.ldrbot.session.GameSessionRepository;
import dev.rubasace.linkedin.games.ldrbot.util.FormatUtils;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.util.Optional;

/**
 * Sends a notification to the group chat when a user establishes a new record
 * (i.e., the fastest time ever registered for a given game in the group).
 */
@Component
public class RecordNotificationService {

    private static final String NEW_RECORD_MESSAGE_TEMPLATE = "\uD83C\uDFC6 <b>New %s record!</b>\n%s set a new group record with a time of <b>%s</b>! \uD83C\uDF89";
    private static final String RECORD_BROKEN_MESSAGE_TEMPLATE = "\uD83C\uDFC6 <b>New %s record!</b>\n%s set a new group record with a time of <b>%s</b> (previous record: %s)! \uD83C\uDF89";

    private static final int RECORD_NOTIFICATION_ORDER = NotificationService.USER_INTERACTION_NOTIFICATION_ORDER + 1;

    private final CustomTelegramClient customTelegramClient;
    private final GameSessionRepository gameSessionRepository;

    RecordNotificationService(final CustomTelegramClient customTelegramClient, final GameSessionRepository gameSessionRepository) {
        this.customTelegramClient = customTelegramClient;
        this.gameSessionRepository = gameSessionRepository;
    }

    @Order(RECORD_NOTIFICATION_ORDER)
    @Async(ExecutorsConfiguration.NOTIFICATION_LISTENER_EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleSessionRegistration(final GameSessionRegistrationEvent event) {
        Duration submittedDuration = event.getDuration();
        Long chatId = event.getChatId();

        Optional<Duration> bestDuration = gameSessionRepository.findBestDuration(chatId, event.getGameType());

        // If the best duration in the DB is not the submitted one, this is not a record
        if (bestDuration.isEmpty() || submittedDuration.compareTo(bestDuration.get()) != 0) {
            return;
        }

        // Find the previous best (second best duration after the current record)
        Optional<Duration> previousBest = gameSessionRepository.findDistinctDurationsOrderedAsc(chatId, event.getGameType())
                .skip(1)  // Skip the first (current best)
                .findFirst();  // Get the second one (previous best)

        String gameName = event.getGameInfo().name();
        String userMention = FormatUtils.formatUserMention(event.getUserInfo());
        String formattedDuration = FormatUtils.formatDuration(submittedDuration);

        if (previousBest.isPresent()) {
            // There was a previous record — show it
            customTelegramClient.sendMessage(
                    RECORD_BROKEN_MESSAGE_TEMPLATE.formatted(gameName, userMention, formattedDuration, FormatUtils.formatDuration(previousBest.get())),
                    event.getChatInfo().chatId()
            );
        } else {
            // First ever submission for this game — it's the first record
            customTelegramClient.sendMessage(
                    NEW_RECORD_MESSAGE_TEMPLATE.formatted(gameName, userMention, formattedDuration),
                    event.getChatInfo().chatId()
            );
        }
    }
}
