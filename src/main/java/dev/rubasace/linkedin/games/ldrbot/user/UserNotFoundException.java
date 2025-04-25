package dev.rubasace.linkedin.games.ldrbot.user;

import dev.rubasace.linkedin.games.ldrbot.chat.UserFeedbackException;
import lombok.Getter;

@Getter
public class UserNotFoundException extends UserFeedbackException {

    private final UserInfo userInfo;


    public UserNotFoundException(final Long chatId, final UserInfo userInfo) {
        super(chatId);
        this.userInfo = userInfo;
    }
}
