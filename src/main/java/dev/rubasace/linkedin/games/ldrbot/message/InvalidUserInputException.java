package dev.rubasace.linkedin.games.ldrbot.message;

import dev.rubasace.linkedin.games.ldrbot.chat.UserFeedbackException;
import lombok.Getter;

@Getter
public class InvalidUserInputException extends UserFeedbackException {

    private final String message;

    public InvalidUserInputException(final String message, final Long chatId) {
        super(chatId);
        this.message = message;
    }
}
