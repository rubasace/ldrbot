package dev.rubasace.linkedin.games_tracker.group;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class GroupCreatedEvent extends ApplicationEvent {

    private final Long chatId;
    private final String groupName;

    public GroupCreatedEvent(final Object source, final Long chatId, final String groupName) {
        super(source);
        this.chatId = chatId;
        this.groupName = groupName;
    }
}
