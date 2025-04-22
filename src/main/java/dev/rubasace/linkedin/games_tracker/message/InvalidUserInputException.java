package dev.rubasace.linkedin.games_tracker.message;

import dev.rubasace.linkedin.games_tracker.chat.UserFeedbackException;
import lombok.Getter;

@Getter
public class InvalidUserInputException extends UserFeedbackException {

    private final String message;

    public InvalidUserInputException(final String message, final Long chatId) {
        super(chatId);
        this.message = message;
    }
}
