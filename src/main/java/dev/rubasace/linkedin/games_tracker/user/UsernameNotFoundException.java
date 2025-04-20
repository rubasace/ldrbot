package dev.rubasace.linkedin.games_tracker.user;

import dev.rubasace.linkedin.games_tracker.chat.UserFeedbackException;
import lombok.Getter;

@Getter
public class UsernameNotFoundException extends UserFeedbackException {

    private final String username;

    public UsernameNotFoundException(final Long chatId, final String username) {
        super(chatId);
        this.username = username;
    }
}
