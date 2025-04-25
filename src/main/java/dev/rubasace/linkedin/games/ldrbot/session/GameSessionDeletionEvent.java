package dev.rubasace.linkedin.games.ldrbot.session;

import dev.rubasace.linkedin.games.ldrbot.group.GroupInfo;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class GameSessionDeletionEvent extends ApplicationEvent {

    private final GroupInfo groupInfo;
    private final UserInfo userInfo;
    private final GameType game;
    private final boolean allGames;

    public GameSessionDeletionEvent(final Object source, final GroupInfo groupInfo, UserInfo userInfo, final GameType game) {
        super(source);
        this.groupInfo = groupInfo;
        this.userInfo = userInfo;
        this.game = game;
        this.allGames = false;
    }

    public GameSessionDeletionEvent(final Object source, final GroupInfo groupInfo, UserInfo userInfo) {
        super(source);
        this.groupInfo = groupInfo;
        this.userInfo = userInfo;
        this.game = null;
        this.allGames = true;
    }

}
