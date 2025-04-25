package dev.rubasace.linkedin.games.ldrbot.group;

import lombok.Getter;

@Getter
public class GroupNotFoundException extends Exception {

    private final GroupInfo groupInfo;

    public GroupNotFoundException(final GroupInfo groupInfo) {
        super("Couldn't find group with id " + groupInfo.chatId());
        this.groupInfo = groupInfo;
    }
}
