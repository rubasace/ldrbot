package dev.rubasace.linkedin.games.ldrbot.chat;

import dev.rubasace.linkedin.games.ldrbot.configuration.ExecutorsConfiguration;
import dev.rubasace.linkedin.games.ldrbot.group.GroupCreatedEvent;
import dev.rubasace.linkedin.games.ldrbot.group.TrackedGamesChangedEvent;
import dev.rubasace.linkedin.games.ldrbot.group.UserJoinedGroupEvent;
import dev.rubasace.linkedin.games.ldrbot.group.UserLeftGroupEvent;
import dev.rubasace.linkedin.games.ldrbot.ranking.GroupDailyScoreCreatedEvent;
import dev.rubasace.linkedin.games.ldrbot.session.GameInfo;
import dev.rubasace.linkedin.games.ldrbot.session.GameSessionDeletionEvent;
import dev.rubasace.linkedin.games.ldrbot.session.GameSessionRegistrationEvent;
import dev.rubasace.linkedin.games.ldrbot.summary.GroupDailyScore;
import dev.rubasace.linkedin.games.ldrbot.util.FormatUtils;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class NotificationService {

    private static final String SUBMISSION_MESSAGE_TEMPLATE = "%s submitted their result for today's %s with a time of %s";
    private static final String GAME_SESSION_DELETION_MESSAGE_TEMPLATE = "%s result for today's %s has been deleted";
    private static final String ALL_SESSION_DELETION_MESSAGE_TEMPLATE = "All %s results for today games have been deleted";
    private static final String USER_JOIN_MESSAGE_TEMPLATE = "%s joined this group";
    private static final String USER_LEAVE_MESSAGE_TEMPLATE = "User %s left this group";

    private static final String GROUP_GREETING_MESSAGE = """
            👋 Hello everyone, I’m LDRBot 🤖!
            
            This group is now officially being tracked 🏁. From now on, you can submit your LinkedIn puzzle screenshots, and I’ll keep score throughout the day.
            
            Every day brings a new competition — submit your time, climb the leaderboard, and make sure you’re not left behind! 🏆
            
            Admins can use /configure to select which games I should track and customize other settings like the group’s timezone.
            
            """ + ChatConstants.HELP_SUGGESTION;

    private static final int GREETING_NOTIFICATION_ORDER = Ordered.HIGHEST_PRECEDENCE;
    private static final int USER_INTERACTION_NOTIFICATION_ORDER = GREETING_NOTIFICATION_ORDER + 1000;
    private static final int DAILY_RANKING_NOTIFICATION_ORDER = 0;

    private final CustomTelegramClient customTelegramClient;
    private final RankingMessageFactory rankingMessageFactory;

    NotificationService(final CustomTelegramClient customTelegramClient, final RankingMessageFactory rankingMessageFactory) {
        this.customTelegramClient = customTelegramClient;
        this.rankingMessageFactory = rankingMessageFactory;
    }


    @Order(GREETING_NOTIFICATION_ORDER)
    @Async(ExecutorsConfiguration.NOTIFICATION_LISTENER_EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleGroupCreation(final GroupCreatedEvent groupCreatedEvent) {
        customTelegramClient.message(GROUP_GREETING_MESSAGE, groupCreatedEvent.getChatInfo().chatId());
    }

    @Order(DAILY_RANKING_NOTIFICATION_ORDER)
    @Async(ExecutorsConfiguration.NOTIFICATION_LISTENER_EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void notifyDailyRanking(final GroupDailyScoreCreatedEvent groupDailyScoreCreatedEvent) {
        GroupDailyScore groupDailyScore = groupDailyScoreCreatedEvent.getGroupDailyScore();
        if (groupDailyScore.globalScore().isEmpty()) {
            customTelegramClient.errorMessage("No games have been registered yet for %s, cannot calculate the ranking".formatted(FormatUtils.formatDate(groupDailyScore.gameDay())),
                                              groupDailyScore.chatInfo().chatId());
            return;
        }

        String htmlSummary = rankingMessageFactory.createRankingMessage(groupDailyScore);
        customTelegramClient.message(htmlSummary, groupDailyScore.chatInfo().chatId());
    }

    @Order(USER_INTERACTION_NOTIFICATION_ORDER)
    @Async(ExecutorsConfiguration.NOTIFICATION_LISTENER_EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleSessionRegistration(final GameSessionRegistrationEvent gameSessionRegistrationEvent) {
        customTelegramClient.message(SUBMISSION_MESSAGE_TEMPLATE.formatted(FormatUtils.formatUserMention(gameSessionRegistrationEvent.getUserInfo()),
                                                                           gameSessionRegistrationEvent.getGameInfo().name(),
                                                                           FormatUtils.formatDuration(gameSessionRegistrationEvent.getDuration())),
                                     gameSessionRegistrationEvent.getChatInfo().chatId());
    }

    @Order(USER_INTERACTION_NOTIFICATION_ORDER)
    @Async(ExecutorsConfiguration.NOTIFICATION_LISTENER_EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleSessionDeletion(final GameSessionDeletionEvent gameSessionDeletionEvent) {
        if (gameSessionDeletionEvent.isAllGames()) {
            customTelegramClient.successMessage(ALL_SESSION_DELETION_MESSAGE_TEMPLATE.formatted(FormatUtils.formatUserMention(gameSessionDeletionEvent.getUserInfo())),
                                                gameSessionDeletionEvent.getChatInfo().chatId());
        } else {
            customTelegramClient.successMessage(
                    GAME_SESSION_DELETION_MESSAGE_TEMPLATE.formatted(FormatUtils.formatUserMention(gameSessionDeletionEvent.getUserInfo()),
                                                                     gameSessionDeletionEvent.getGameInfo().name().toLowerCase()),
                    gameSessionDeletionEvent.getChatInfo().chatId());
        }
    }

    @Order(USER_INTERACTION_NOTIFICATION_ORDER)
    @Async(ExecutorsConfiguration.NOTIFICATION_LISTENER_EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleUserJoin(final UserJoinedGroupEvent userJoinedGroupEvent) {
        customTelegramClient.message(USER_JOIN_MESSAGE_TEMPLATE.formatted(FormatUtils.formatUserMention(userJoinedGroupEvent.getUserInfo())),
                                     userJoinedGroupEvent.getChatInfo().chatId());
    }

    @Order(USER_INTERACTION_NOTIFICATION_ORDER)
    @Async(ExecutorsConfiguration.NOTIFICATION_LISTENER_EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleUserLeave(final UserLeftGroupEvent userLeftGroupEvent) {
        customTelegramClient.message(USER_LEAVE_MESSAGE_TEMPLATE.formatted(FormatUtils.formatUserMention(userLeftGroupEvent.getUserInfo())),
                                     userLeftGroupEvent.getChatInfo().chatId());
    }

    @Order(USER_INTERACTION_NOTIFICATION_ORDER)
    @Async(ExecutorsConfiguration.NOTIFICATION_LISTENER_EXECUTOR_NAME)
    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleTrackedGamesChanged(final TrackedGamesChangedEvent trackedGamesChangedEvent) {
        Set<GameInfo> trackedGames = trackedGamesChangedEvent.getTrackedGames();
        if (CollectionUtils.isEmpty(trackedGames)) {
            customTelegramClient.errorMessage("This group is not tracking any games.", trackedGamesChangedEvent.getChatId());
        } else {
            String text = trackedGames.stream()
                                      .sorted(Comparator.comparing(GameInfo::name))
                                      .map(gameInfo -> "%s %s".formatted(gameInfo.icon(), gameInfo.name()))
                                      .collect(Collectors.joining("\n"));

            customTelegramClient.message("This group is currently tracking:\n" + text, trackedGamesChangedEvent.getChatId());
        }
    }

}
