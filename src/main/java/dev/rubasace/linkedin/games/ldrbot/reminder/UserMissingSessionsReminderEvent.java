package dev.rubasace.linkedin.games.ldrbot.reminder;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserMissingSessionsReminderEvent extends ApplicationEvent {
    private final String userName;
    private final Long chatId;

    public UserMissingSessionsReminderEvent(final Object source, final String userName, final Long chatId) {
        super(source);
        this.userName = userName;
        this.chatId = chatId;
    }
}
