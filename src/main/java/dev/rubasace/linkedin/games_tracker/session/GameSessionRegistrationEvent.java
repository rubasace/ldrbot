package dev.rubasace.linkedin.games_tracker.session;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class GameSessionRegistrationEvent extends ApplicationEvent {

    private final Long userId;
    private final Long chatId;

    public GameSessionRegistrationEvent(final Object source, final Long userId, final Long chatId) {
        super(source);
        this.userId = userId;
        this.chatId = chatId;
    }

}
