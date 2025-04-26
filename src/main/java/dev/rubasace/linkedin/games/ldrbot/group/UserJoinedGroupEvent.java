package dev.rubasace.linkedin.games.ldrbot.group;

import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserJoinedGroupEvent extends ApplicationEvent {

    private final UserInfo userInfo;
    private final ChatInfo chatInfo;

    public UserJoinedGroupEvent(final Object source, final UserInfo userInfo, final ChatInfo chatInfo) {
        super(source);
        this.userInfo = userInfo;
        this.chatInfo = chatInfo;
    }
}
