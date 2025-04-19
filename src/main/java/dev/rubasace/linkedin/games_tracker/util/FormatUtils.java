package dev.rubasace.linkedin.games_tracker.util;

import java.time.Duration;

public class FormatUtils {

    public static String formatDuration(Duration d) {
        long minutes = d.toMinutes();
        long seconds = d.minusMinutes(minutes).getSeconds();
        return String.format("%02d:%02d", minutes, seconds);
    }
}
