package dev.rubasace.linkedin.games_tracker.session;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class GameSessionRegistrationEvent extends ApplicationEvent {

    private final Long userId;

    public GameSessionRegistrationEvent(final Object source, final Long userId) {
        super(source);
        this.userId = userId;
    }

}
