package dev.rubasace.linkedin.games.ldrbot.group;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.ZoneId;

@Getter
public class TimezoneChangedEvent extends ApplicationEvent {
    private final Long chatId;
    private final ZoneId timezone;

    public TimezoneChangedEvent(final Object source, final Long chatId, final ZoneId timezone) {
        super(source);
        this.chatId = chatId;
        this.timezone = timezone;
    }
}
