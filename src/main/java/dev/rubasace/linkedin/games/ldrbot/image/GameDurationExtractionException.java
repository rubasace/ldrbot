package dev.rubasace.linkedin.games.ldrbot.image;

import dev.rubasace.linkedin.games.ldrbot.chat.UserFeedbackException;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import lombok.Getter;

@Getter
public class GameDurationExtractionException extends UserFeedbackException {

    private final String userName;
    private final GameType gameType;

    public GameDurationExtractionException(final Long chatId, final String userName, final GameType gameType) {
        super(chatId);
        this.userName = userName;
        this.gameType = gameType;
    }
}
