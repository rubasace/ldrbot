package dev.rubasace.linkedin.games.ldrbot.text;

import dev.rubasace.linkedin.games.ldrbot.chat.UserFeedbackException;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import lombok.Getter;

@Getter
public class TextGameDurationExtractionException extends UserFeedbackException {

    private final UserInfo userInfo;
    private final GameType gameType;

    public TextGameDurationExtractionException(final Long chatId, final UserInfo userInfo, final GameType gameType) {
        super(chatId);
        this.userInfo = userInfo;
        this.gameType = gameType;
    }
}
