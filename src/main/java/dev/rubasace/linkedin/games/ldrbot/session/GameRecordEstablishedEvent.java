package dev.rubasace.linkedin.games.ldrbot.session;

import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Duration;

@Getter
public class GameRecordEstablishedEvent extends ApplicationEvent {

    private final ChatInfo chatInfo;
    private final UserInfo userInfo;
    private final GameInfo gameInfo;
    private final Duration duration;
    private final Duration bestOtherDuration; // nullable: null == no other session exists (first-ever record)

    public GameRecordEstablishedEvent(final Object source, final ChatInfo chatInfo, final UserInfo userInfo,
                                      final GameInfo gameInfo, final Duration duration, final Duration bestOtherDuration) {
        super(source);
        this.chatInfo = chatInfo;
        this.userInfo = userInfo;
        this.gameInfo = gameInfo;
        this.duration = duration;
        this.bestOtherDuration = bestOtherDuration;
    }
}
