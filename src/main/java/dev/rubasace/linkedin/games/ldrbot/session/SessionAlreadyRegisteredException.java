package dev.rubasace.linkedin.games.ldrbot.session;


import dev.rubasace.linkedin.games.ldrbot.chat.UserFeedbackException;
import dev.rubasace.linkedin.games.ldrbot.group.GroupInfo;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import lombok.Getter;

@Getter
public class SessionAlreadyRegisteredException extends UserFeedbackException {

    private final GroupInfo groupInfo;
    private final UserInfo userInfo;
    private final GameType gameType;


    public SessionAlreadyRegisteredException(final GroupInfo groupInfo, final UserInfo userInfo, final GameType gameType) {
        super(groupInfo.chatId());
        this.groupInfo = groupInfo;
        this.userInfo = userInfo;
        this.gameType = gameType;
    }
}
