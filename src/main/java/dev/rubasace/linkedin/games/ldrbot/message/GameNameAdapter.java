package dev.rubasace.linkedin.games.ldrbot.message;

import dev.rubasace.linkedin.games.ldrbot.session.GameNameNotFoundException;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import org.springframework.stereotype.Component;

@Component
public class GameNameAdapter {

    public GameType adapt(final String gameName, final Long chatId) throws GameNameNotFoundException {
        GameType gameType;
        try {
            gameType = GameType.valueOf(gameName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new GameNameNotFoundException(chatId, gameName);
        }
        return gameType;
    }
}
