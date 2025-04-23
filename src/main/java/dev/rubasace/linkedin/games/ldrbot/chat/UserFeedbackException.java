package dev.rubasace.linkedin.games.ldrbot.chat;

import lombok.Getter;

/**
 * Base exception class used to indicate that some feedback has to be given back to the client
 */
@Getter
public class UserFeedbackException extends Exception {

    private final Long chatId;

    public UserFeedbackException(final Long chatId) {
        this.chatId = chatId;
    }
}
