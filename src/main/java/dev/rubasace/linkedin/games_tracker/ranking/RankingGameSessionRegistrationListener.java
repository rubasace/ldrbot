package dev.rubasace.linkedin.games_tracker.ranking;

import dev.rubasace.linkedin.games_tracker.configuration.AsyncConfiguration;
import dev.rubasace.linkedin.games_tracker.session.GameSessionRegistrationEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class RankingGameSessionRegistrationListener {


    private final GroupsRankingReadinessCheckService groupsRankingReadinessCheckService;

    RankingGameSessionRegistrationListener(final GroupsRankingReadinessCheckService groupsRankingReadinessCheckService) {
        this.groupsRankingReadinessCheckService = groupsRankingReadinessCheckService;
    }

    @Async(AsyncConfiguration.EVENT_LISTENER_EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleSessionRegistration(final GameSessionRegistrationEvent gameSessionRegistrationEvent) {
        groupsRankingReadinessCheckService.process(gameSessionRegistrationEvent);
    }
}
