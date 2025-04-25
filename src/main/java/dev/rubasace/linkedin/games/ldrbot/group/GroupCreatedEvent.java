package dev.rubasace.linkedin.games.ldrbot.group;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class GroupCreatedEvent extends ApplicationEvent {

    private final GroupInfo groupInfo;

    public GroupCreatedEvent(final Object source, final GroupInfo groupInfo) {
        super(source);
        this.groupInfo = groupInfo;
    }
}
