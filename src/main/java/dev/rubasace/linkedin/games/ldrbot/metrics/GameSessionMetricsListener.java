package dev.rubasace.linkedin.games.ldrbot.metrics;

import dev.rubasace.linkedin.games.ldrbot.session.GameSessionDeletionEvent;
import dev.rubasace.linkedin.games.ldrbot.session.GameSessionRegistrationEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class GameSessionMetricsListener {

    private final MeterRegistry meterRegistry;
    private final Counter sessionsDeletedCounter;

    GameSessionMetricsListener(final MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.sessionsDeletedCounter = meterRegistry.counter(MetricsConstants.SESSIONS_DELETED);
    }

    @EventListener
    public void onSessionRegistered(GameSessionRegistrationEvent event) {
        String gameType = event.getGameInfo() != null ? event.getGameInfo().name() : "unknown";
        meterRegistry.counter(MetricsConstants.SESSIONS_REGISTERED,
                MetricsConstants.TAG_GAME_TYPE, gameType)
                     .increment();
    }

    @EventListener
    public void onSessionDeleted(GameSessionDeletionEvent event) {
        sessionsDeletedCounter.increment();
    }
}
