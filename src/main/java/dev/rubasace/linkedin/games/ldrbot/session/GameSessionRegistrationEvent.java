package dev.rubasace.linkedin.games.ldrbot.session;

import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Duration;
import java.time.LocalDate;

@Getter
public class GameSessionRegistrationEvent extends ApplicationEvent {

    private final ChatInfo chatInfo;
    private final UserInfo userInfo;
    private final GameInfo gameInfo;
    private final Duration duration;
    private final LocalDate gameDay;
    private final Long chatId;

    public GameSessionRegistrationEvent(final Object source, final ChatInfo chatInfo, final UserInfo userInfo, final GameInfo gameInfo, final Duration duration, final LocalDate gameDay, final Long chatId) {
        super(source);
        this.chatInfo = chatInfo;
        this.userInfo = userInfo;
        this.gameInfo = gameInfo;
        this.duration = duration;
        this.gameDay = gameDay;
        this.chatId = chatId;
    }

}
