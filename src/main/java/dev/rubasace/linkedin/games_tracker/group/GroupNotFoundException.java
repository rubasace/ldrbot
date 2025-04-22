package dev.rubasace.linkedin.games_tracker.group;

import lombok.Getter;

@Getter
public class GroupNotFoundException extends Exception {

    private final Long chatId;

    public GroupNotFoundException(final Long chatId) {
        super("Couldn't find group with id " + chatId);
        this.chatId = chatId;
    }
}
