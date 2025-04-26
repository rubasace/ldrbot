package dev.rubasace.linkedin.games.ldrbot.session;

import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class GameSessionDeletionEvent extends ApplicationEvent {

    private final ChatInfo chatInfo;
    private final UserInfo userInfo;
    private final GameType game;
    private final boolean allGames;

    public GameSessionDeletionEvent(final Object source, final ChatInfo chatInfo, UserInfo userInfo, final GameType game) {
        super(source);
        this.chatInfo = chatInfo;
        this.userInfo = userInfo;
        this.game = game;
        this.allGames = false;
    }

    public GameSessionDeletionEvent(final Object source, final ChatInfo chatInfo, UserInfo userInfo) {
        super(source);
        this.chatInfo = chatInfo;
        this.userInfo = userInfo;
        this.game = null;
        this.allGames = true;
    }

}
