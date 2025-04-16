package dev.rubasace.linkedin.games_tracker.util;

import java.time.Duration;

public class FormatUtils {

    public static String formatDuration(Duration duration) {
        long minutes = duration.toMinutes();
        long seconds = duration.minusMinutes(minutes).getSeconds();

        StringBuilder sb = new StringBuilder();
        if (minutes > 0) {
            sb.append(minutes).append(" minute").append(minutes != 1 ? "s" : "");
        }
        if (minutes > 0 && seconds > 0) {
            sb.append(" and ");
        }
        if (seconds > 0 || (minutes == 0 && seconds == 0)) {
            sb.append(seconds).append(" second").append(seconds != 1 ? "s" : "");
        }

        return sb.toString();
    }
}
