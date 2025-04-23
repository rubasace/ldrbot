package dev.rubasace.linkedin.games.ldrbot.session;

import dev.rubasace.linkedin.games.ldrbot.chat.UserFeedbackException;
import lombok.Getter;

@Getter
public class GameNameNotFoundException extends UserFeedbackException {

    private final String gameName;

    public GameNameNotFoundException(final Long chatId, final String gameName) {
        super(chatId);
        this.gameName = gameName;
    }
}
