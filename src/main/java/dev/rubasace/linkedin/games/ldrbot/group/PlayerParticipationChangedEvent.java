package dev.rubasace.linkedin.games.ldrbot.group;

import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * A player started or stopped taking part in a group's daily competition, whichever way it was triggered — an admin
 * toggle, an automatic return on a submitted result, or a departure from the group.
 * <p>
 * {@code parked} is the state on today's game day <em>after</em> the change and {@code playersTakingPart} is the size
 * of the participating set on today's game day after it too. The answers travel on the event; the periods never do.
 */
@Getter
public class PlayerParticipationChangedEvent extends ApplicationEvent {

    private final Long chatId;
    private final UserInfo player;
    private final boolean parked;
    private final int playersTakingPart;

    public PlayerParticipationChangedEvent(final Object source, final Long chatId, final UserInfo player, final boolean parked, final int playersTakingPart) {
        super(source);
        this.chatId = chatId;
        this.player = player;
        this.parked = parked;
        this.playersTakingPart = playersTakingPart;
    }
}
