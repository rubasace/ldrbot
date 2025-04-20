package dev.rubasace.linkedin.games_tracker.group;

import dev.rubasace.linkedin.games_tracker.chat.UserFeedbackException;

public class GroupNotFoundException extends UserFeedbackException {

    public GroupNotFoundException(final Long chatId) {
        super(chatId);
    }
}
