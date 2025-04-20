package dev.rubasace.linkedin.games_tracker.util;

import dev.rubasace.linkedin.games_tracker.session.GameType;

import java.time.Duration;

public class FormatUtils {

    public static String formatDuration(Duration d) {
        long minutes = d.toMinutes();
        long seconds = d.minusMinutes(minutes).getSeconds();
        return String.format("%02d:%02d", minutes, seconds);
    }

    public static String gameIcon(final GameType gameType) {
        return switch (gameType) {
            case ZIP -> "🏁";
            case TANGO -> "🌙";
            case QUEENS -> "👑";
        };
    }
}
