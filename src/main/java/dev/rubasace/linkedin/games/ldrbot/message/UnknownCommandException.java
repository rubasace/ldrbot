package dev.rubasace.linkedin.games.ldrbot.message;

import dev.rubasace.linkedin.games.ldrbot.chat.UserFeedbackException;
import lombok.Getter;

@Getter
public class UnknownCommandException extends UserFeedbackException {

    private final String command;

    public UnknownCommandException(final Long chatId, final String command) {
        super(chatId);
        this.command = command;
    }
}
