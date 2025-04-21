package dev.rubasace.linkedin.games_tracker.group;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserLeftGroupEvent extends ApplicationEvent {

    private final Long userId;
    private final String userName;
    private final Long chatId;

    public UserLeftGroupEvent(final Object source, final Long userId, final String userName, final Long chatId) {
        super(source);
        this.userId = userId;
        this.userName = userName;
        this.chatId = chatId;
    }
}
