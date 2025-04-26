package dev.rubasace.linkedin.games.ldrbot.session;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class GameTypeAdapter {

    public GameInfo adapt(final GameType gameType) {
        return new GameInfo(StringUtils.capitalize(gameType.name().toLowerCase()), gameIcon(gameType));
    }

    private static String gameIcon(final GameType gameType) {
        return switch (gameType) {
            case ZIP -> "🏁";
            case TANGO -> "🌙";
            case QUEENS -> "👑";
        };
    }
}
