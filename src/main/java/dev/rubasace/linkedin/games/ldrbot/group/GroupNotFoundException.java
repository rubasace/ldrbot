package dev.rubasace.linkedin.games.ldrbot.group;

import lombok.Getter;

@Getter
public class GroupNotFoundException extends Exception {

    private final ChatInfo chatInfo;

    public GroupNotFoundException(final ChatInfo chatInfo) {
        super("Couldn't find group with id " + chatInfo.chatId());
        this.chatInfo = chatInfo;
    }
}
