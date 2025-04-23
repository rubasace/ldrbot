package dev.rubasace.linkedin.games.ldrbot.group;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserJoinedGroupEvent extends ApplicationEvent {

    private final Long userId;
    private final String userName;
    private final Long chatId;

    public UserJoinedGroupEvent(final Object source, final Long userId, final String userName, final Long chatId) {
        super(source);
        this.userId = userId;
        this.userName = userName;
        this.chatId = chatId;
    }
}
