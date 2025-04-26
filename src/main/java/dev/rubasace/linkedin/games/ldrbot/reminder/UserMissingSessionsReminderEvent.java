package dev.rubasace.linkedin.games.ldrbot.reminder;

import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserMissingSessionsReminderEvent extends ApplicationEvent {

    private final ChatInfo chatInfo;
    private final UserInfo userInfo;

    public UserMissingSessionsReminderEvent(final Object source, final ChatInfo chatInfo, final UserInfo userInfo) {
        super(source);
        this.chatInfo = chatInfo;
        this.userInfo = userInfo;
    }
}
