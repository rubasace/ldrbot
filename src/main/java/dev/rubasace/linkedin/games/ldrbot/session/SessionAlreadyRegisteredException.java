package dev.rubasace.linkedin.games.ldrbot.session;


import dev.rubasace.linkedin.games.ldrbot.chat.UserFeedbackException;
import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import lombok.Getter;

@Getter
public class SessionAlreadyRegisteredException extends UserFeedbackException {

    private final ChatInfo chatInfo;
    private final UserInfo userInfo;
    private final GameInfo gameInfo;


    public SessionAlreadyRegisteredException(final ChatInfo chatInfo, final UserInfo userInfo, final GameInfo gameInfo) {
        super(chatInfo.chatId());
        this.chatInfo = chatInfo;
        this.userInfo = userInfo;
        this.gameInfo = gameInfo;
    }
}
