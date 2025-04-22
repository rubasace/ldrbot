package dev.rubasace.linkedin.games_tracker.chat;

import dev.rubasace.linkedin.games_tracker.configuration.ExecutorsConfiguration;
import dev.rubasace.linkedin.games_tracker.group.GroupCreatedEvent;
import dev.rubasace.linkedin.games_tracker.group.UserJoinedGroupEvent;
import dev.rubasace.linkedin.games_tracker.group.UserLeftGroupEvent;
import dev.rubasace.linkedin.games_tracker.image.GameDurationExtractionException;
import dev.rubasace.linkedin.games_tracker.ranking.GroupDailyScoreCreatedEvent;
import dev.rubasace.linkedin.games_tracker.session.AlreadyRegisteredSession;
import dev.rubasace.linkedin.games_tracker.session.GameNameNotFoundException;
import dev.rubasace.linkedin.games_tracker.session.GameSessionDeletionEvent;
import dev.rubasace.linkedin.games_tracker.session.GameSessionRegistrationEvent;
import dev.rubasace.linkedin.games_tracker.summary.GroupDailyScore;
import dev.rubasace.linkedin.games_tracker.user.UsernameNotFoundException;
import dev.rubasace.linkedin.games_tracker.util.FormatUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

//TODO think about decoupling chatId and user data from events and store them as part of the thread so it's available without having to pass it through (careful with executors)
@Component
public class NotificationService {

    private static final String ALREADY_REGISTERED_SESSION_MESSAGE_TEMPLATE = "@%s already registered a time for %s. If you need to override the time, please delete the current time through the \"/delete <game>\" command. In this case: /delete %s. Alternatively, you can delete all your submissions for the day using /deleteall";
    private static final String SUBMISSION_MESSAGE_TEMPLATE = "@%s submitted their result for today's %s with a time of %s";

    private static final String GAME_SESSION_DELETION_MESSAGE_TEMPLATE = "@%s result for today's %s has been deleted";
    private static final String ALL_SESSION_DELETION_MESSAGE_TEMPLATE = "All @%s results for today games have been deleted";
    private static final String USER_JOIN_MESSAGE_TEMPLATE = "User @%s joined this group";
    private static final String USER_LEAVE_MESSAGE_TEMPLATE = "User @%s left this group";
    private static final String GROUP_GREETING_MESSAGE = """
            👋 Hey everyone, I'm your LinkedIn Games Tracker bot 🤖!
            
            This group is now officially being tracked 🏁. From now on, you can submit your LinkedIn puzzle screenshots, and I’ll keep score for the day.
            
            Every day is a new competition — submit your time, climb the leaderboard, and don’t get left behind! 💪
            
            Type /help to see everything I can do.
            """;
    private static final int GREETING_NOTIFICATION_ORDER = Ordered.HIGHEST_PRECEDENCE;
    private static final int USER_INTERACTION_NOTIFICATION_ORDER = GREETING_NOTIFICATION_ORDER + 1000;
    private static final int DAILY_RANKING_NOTIFICATION_ORDER = 0;

    private final MessageService messageService;
    private final RankingMessageFactory rankingMessageFactory;

    NotificationService(final MessageService messageService, final RankingMessageFactory rankingMessageFactory) {
        this.messageService = messageService;
        this.rankingMessageFactory = rankingMessageFactory;
    }

    public void notifyUserFeedbackException(final UserFeedbackException userFeedbackException) {
        if (userFeedbackException instanceof AlreadyRegisteredSession alreadyRegisteredSession) {
            messageService.error(ALREADY_REGISTERED_SESSION_MESSAGE_TEMPLATE.formatted(alreadyRegisteredSession.getUsername(),
                                                                                       alreadyRegisteredSession.getGame().name(),
                                                                                       alreadyRegisteredSession.getGame().name().toLowerCase()),
                                 alreadyRegisteredSession.getChatId());
        } else if (userFeedbackException instanceof UsernameNotFoundException usernameNotFoundException) {
            messageService.error("User @%s not found".formatted(usernameNotFoundException.getUsername()), usernameNotFoundException.getChatId());
        } else if (userFeedbackException instanceof GameNameNotFoundException gameNameNotFoundException) {
            messageService.error("'%s' is not a valid game.".formatted(gameNameNotFoundException.getGameName()), gameNameNotFoundException.getChatId());
        } else if (userFeedbackException instanceof GameDurationExtractionException gameDurationExtractionException) {
            messageService.error(
                    "@%s submitted a screenshot for the game %s but I wasn't able to extract the time. Please try with another image or ask an admin to input the time manually using the command /override %s <time>".formatted(
                            gameDurationExtractionException.getUserName(), gameDurationExtractionException.getGameType().name(),
                            gameDurationExtractionException.getGameType().name().toLowerCase()),
                    gameDurationExtractionException.getChatId());
        } else if (userFeedbackException instanceof InvalidUserInputException invalidUserInputException) {
            messageService.error(invalidUserInputException.getMessage(), invalidUserInputException.getChatId());
        }
    }

    @Order(GREETING_NOTIFICATION_ORDER)
    @Async(ExecutorsConfiguration.NOTIFICATION_LISTENER_EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleGroupCreation(final GroupCreatedEvent groupCreatedEvent) {
        messageService.info(GROUP_GREETING_MESSAGE, groupCreatedEvent.getChatId());
    }

    @Order(DAILY_RANKING_NOTIFICATION_ORDER)
    @Async(ExecutorsConfiguration.NOTIFICATION_LISTENER_EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void notifyDailyRanking(final GroupDailyScoreCreatedEvent groupDailyScoreCreatedEvent) {
        GroupDailyScore groupDailyScore = groupDailyScoreCreatedEvent.getGroupDailyScore();
        if (groupDailyScore.globalScore().isEmpty()) {
            messageService.error("No games have been registered yet, cannot calculate te ranking", groupDailyScore.chatId());
            return;
        }

        String htmlSummary = rankingMessageFactory.createRankingMessage(groupDailyScore);
        messageService.html(htmlSummary, groupDailyScore.chatId());
    }

    @Order(USER_INTERACTION_NOTIFICATION_ORDER)
    @Async(ExecutorsConfiguration.NOTIFICATION_LISTENER_EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleSessionRegistration(final GameSessionRegistrationEvent gameSessionRegistrationEvent) {
        messageService.info(SUBMISSION_MESSAGE_TEMPLATE.formatted(gameSessionRegistrationEvent.getUserName(),
                                                                  gameSessionRegistrationEvent.getGame().name().toLowerCase(),
                                                                  FormatUtils.formatDuration(gameSessionRegistrationEvent.getDuration())),
                            gameSessionRegistrationEvent.getChatId());
    }

    @Order(USER_INTERACTION_NOTIFICATION_ORDER)
    @Async(ExecutorsConfiguration.NOTIFICATION_LISTENER_EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleSessionDeletion(final GameSessionDeletionEvent gameSessionDeletionEvent) {
        if (gameSessionDeletionEvent.isAllGames()) {
            messageService.success(ALL_SESSION_DELETION_MESSAGE_TEMPLATE.formatted(gameSessionDeletionEvent.getUserName()), gameSessionDeletionEvent.getChatId());
        } else {
            messageService.success(
                    GAME_SESSION_DELETION_MESSAGE_TEMPLATE.formatted(gameSessionDeletionEvent.getUserName(), gameSessionDeletionEvent.getGame().name().toLowerCase()),
                    gameSessionDeletionEvent.getChatId());
        }
    }

    @Order(USER_INTERACTION_NOTIFICATION_ORDER)
    @Async(ExecutorsConfiguration.NOTIFICATION_LISTENER_EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleUserJoin(final UserJoinedGroupEvent userJoinedGroupEvent) {
        messageService.info(USER_JOIN_MESSAGE_TEMPLATE.formatted(userJoinedGroupEvent.getUserName()),
                            userJoinedGroupEvent.getChatId());
    }

    @Order(USER_INTERACTION_NOTIFICATION_ORDER)
    @Async(ExecutorsConfiguration.NOTIFICATION_LISTENER_EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleUserLeave(final UserLeftGroupEvent userLeftGroupEvent) {
        messageService.info(USER_LEAVE_MESSAGE_TEMPLATE.formatted(userLeftGroupEvent.getUserName()),
                            userLeftGroupEvent.getChatId());
    }

}
