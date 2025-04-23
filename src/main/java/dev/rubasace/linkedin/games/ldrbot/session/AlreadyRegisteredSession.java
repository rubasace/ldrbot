package dev.rubasace.linkedin.games.ldrbot.session;


import dev.rubasace.linkedin.games.ldrbot.chat.UserFeedbackException;
import lombok.Getter;

@Getter
public class AlreadyRegisteredSession extends UserFeedbackException {

    private final String username;
    private final GameType game;


    public AlreadyRegisteredSession(final String username, final GameType game, final Long chatId) {
        super(chatId);
        this.username = username;
        this.game = game;
    }

}
