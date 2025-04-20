package dev.rubasace.linkedin.games_tracker.session;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Duration;

@Getter
public class GameSessionRegistrationEvent extends ApplicationEvent {

    private final Long userId;
    private final String userName;
    private final GameType game;
    private final Duration duration;
    private final Long chatId;

    public GameSessionRegistrationEvent(final Object source, final Long userId, final String userName, final GameType game, final Duration duration, final Long chatId) {
        super(source);
        this.userId = userId;
        this.userName = userName;
        this.game = game;
        this.duration = duration;
        this.chatId = chatId;
    }

}
