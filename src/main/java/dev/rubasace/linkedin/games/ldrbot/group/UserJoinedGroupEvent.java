package dev.rubasace.linkedin.games.ldrbot.group;

import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserJoinedGroupEvent extends ApplicationEvent {

    private final UserInfo userInfo;
    private final GroupInfo groupInfo;

    public UserJoinedGroupEvent(final Object source, final UserInfo userInfo, final GroupInfo groupInfo) {
        super(source);
        this.userInfo = userInfo;
        this.groupInfo = groupInfo;
    }
}
