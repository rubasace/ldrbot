package dev.rubasace.linkedin.games.ldrbot.group;

import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserLeftGroupEvent extends ApplicationEvent {

    private final GroupInfo groupInfo;
    private final UserInfo userInfo;

    public UserLeftGroupEvent(final Object source, final GroupInfo groupInfo, final UserInfo userInfo) {
        super(source);

        this.groupInfo = groupInfo;
        this.userInfo = userInfo;
    }
}
