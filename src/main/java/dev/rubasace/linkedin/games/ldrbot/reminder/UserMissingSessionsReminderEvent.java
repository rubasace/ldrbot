package dev.rubasace.linkedin.games.ldrbot.reminder;

import dev.rubasace.linkedin.games.ldrbot.group.GroupInfo;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserMissingSessionsReminderEvent extends ApplicationEvent {

    private final GroupInfo groupInfo;
    private final UserInfo userInfo;

    public UserMissingSessionsReminderEvent(final Object source, final GroupInfo groupInfo, final UserInfo userInfo) {
        super(source);
        this.groupInfo = groupInfo;
        this.userInfo = userInfo;
    }
}
