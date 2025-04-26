package dev.rubasace.linkedin.games.ldrbot.session;

import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class GameSessionDeletionEvent extends ApplicationEvent {

    private final ChatInfo chatInfo;
    private final UserInfo userInfo;
    private final GameInfo gameInfo;
    private final boolean allGames;

    public GameSessionDeletionEvent(final Object source, final ChatInfo chatInfo, UserInfo userInfo, final GameInfo gameInfo) {
        super(source);
        this.chatInfo = chatInfo;
        this.userInfo = userInfo;
        this.gameInfo = gameInfo;
        this.allGames = false;
    }

    public GameSessionDeletionEvent(final Object source, final ChatInfo chatInfo, UserInfo userInfo) {
        super(source);
        this.chatInfo = chatInfo;
        this.userInfo = userInfo;
        this.gameInfo = null;
        this.allGames = true;
    }

}
