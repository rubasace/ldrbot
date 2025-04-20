package dev.rubasace.linkedin.games_tracker.session;

import dev.rubasace.linkedin.games_tracker.chat.UserFeedbackException;
import lombok.Getter;

@Getter
public class GameNameNotFoundException extends UserFeedbackException {

    private final String gameName;

    public GameNameNotFoundException(final Long chatId, final String gameName) {
        super(chatId);
        this.gameName = gameName;
    }
}
