package dev.rubasace.linkedin.games.ldrbot.ranking;

import dev.rubasace.linkedin.games.ldrbot.configuration.ExecutorsConfiguration;
import dev.rubasace.linkedin.games.ldrbot.session.GameSessionRegistrationEvent;
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

    @Async(ExecutorsConfiguration.BACKGROUND_TASKS_EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleSessionRegistration(final GameSessionRegistrationEvent gameSessionRegistrationEvent) {
        groupsRankingReadinessCheckService.process(gameSessionRegistrationEvent);
    }
}
