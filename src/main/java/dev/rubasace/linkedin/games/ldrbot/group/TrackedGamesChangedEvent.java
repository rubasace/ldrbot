package dev.rubasace.linkedin.games.ldrbot.group;

import dev.rubasace.linkedin.games.ldrbot.session.GameInfo;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Set;

@Getter
public class TrackedGamesChangedEvent extends ApplicationEvent {
    private final Long chatId;
    private final Set<GameInfo> trackedGames;

    public TrackedGamesChangedEvent(final Object source, final Long chatId, final Set<GameInfo> trackedGames) {
        super(source);
        this.chatId = chatId;
        this.trackedGames = trackedGames;
    }
}
