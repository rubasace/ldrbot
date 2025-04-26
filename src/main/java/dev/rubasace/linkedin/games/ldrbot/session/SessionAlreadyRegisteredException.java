package dev.rubasace.linkedin.games.ldrbot.session;


import dev.rubasace.linkedin.games.ldrbot.chat.UserFeedbackException;
import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import lombok.Getter;

@Getter
public class SessionAlreadyRegisteredException extends UserFeedbackException {

    private final ChatInfo chatInfo;
    private final UserInfo userInfo;
    private final GameType gameType;


    public SessionAlreadyRegisteredException(final ChatInfo chatInfo, final UserInfo userInfo, final GameType gameType) {
        super(chatInfo.chatId());
        this.chatInfo = chatInfo;
        this.userInfo = userInfo;
        this.gameType = gameType;
    }
}
