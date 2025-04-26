package dev.rubasace.linkedin.games.ldrbot.group;

import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserLeftGroupEvent extends ApplicationEvent {

    private final ChatInfo chatInfo;
    private final UserInfo userInfo;

    public UserLeftGroupEvent(final Object source, final ChatInfo chatInfo, final UserInfo userInfo) {
        super(source);

        this.chatInfo = chatInfo;
        this.userInfo = userInfo;
    }
}
