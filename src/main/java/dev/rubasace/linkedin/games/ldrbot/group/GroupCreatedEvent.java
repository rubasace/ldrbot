package dev.rubasace.linkedin.games.ldrbot.group;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class GroupCreatedEvent extends ApplicationEvent {

    private final ChatInfo chatInfo;

    public GroupCreatedEvent(final Object source, final ChatInfo chatInfo) {
        super(source);
        this.chatInfo = chatInfo;
    }
}
