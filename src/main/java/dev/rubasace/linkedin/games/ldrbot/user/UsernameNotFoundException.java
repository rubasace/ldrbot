package dev.rubasace.linkedin.games.ldrbot.user;

import dev.rubasace.linkedin.games.ldrbot.chat.UserFeedbackException;
import lombok.Getter;

@Getter
public class UsernameNotFoundException extends UserFeedbackException {

    private final String username;

    public UsernameNotFoundException(final Long chatId, final String username) {
        super(chatId);
        this.username = username;
    }
}
