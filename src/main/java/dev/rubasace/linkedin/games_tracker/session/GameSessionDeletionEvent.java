package dev.rubasace.linkedin.games_tracker.session;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class GameSessionDeletionEvent extends ApplicationEvent {

    private final Long userId;
    private final String userName;
    private final GameType game;
    private final Long chatId;
    private final boolean allGames;

    public GameSessionDeletionEvent(final Object source, final Long userId, final String userName, final GameType game, final Long chatId) {
        super(source);
        this.userId = userId;
        this.userName = userName;
        this.game = game;
        this.chatId = chatId;
        this.allGames = false;
    }

    public GameSessionDeletionEvent(final Object source, final Long userId, final String userName, final Long chatId) {
        super(source);
        this.userId = userId;
        this.userName = userName;
        this.game = null;
        this.chatId = chatId;
        this.allGames = true;
    }

}
