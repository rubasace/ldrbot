package dev.rubasace.linkedin.games.ldrbot.group;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

@Getter
public class StartTimeResetEvent extends ApplicationEvent {
    private final Long chatId;
    private final LocalDateTime resetTime;

    public StartTimeResetEvent(final Object source, final Long chatId, final LocalDateTime resetTime) {
        super(source);
        this.chatId = chatId;
        this.startDate = resetTime;
    }
}
