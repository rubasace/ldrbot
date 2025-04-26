package dev.rubasace.linkedin.games.ldrbot.session;

import dev.rubasace.linkedin.games.ldrbot.group.UserLeftGroupEvent;
import dev.rubasace.linkedin.games.ldrbot.util.LinkedinTimeUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class GameSessionEventListener {

    private final GameSessionService gameSessionService;

    GameSessionEventListener(final GameSessionService gameSessionService) {
        this.gameSessionService = gameSessionService;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleUserLeftGroupEvent(final UserLeftGroupEvent userLeftGroupEvent) {
        gameSessionService.deleteDaySessions(userLeftGroupEvent.getChatInfo(), userLeftGroupEvent.getUserInfo(), LinkedinTimeUtils.todayGameDay());
    }
}
