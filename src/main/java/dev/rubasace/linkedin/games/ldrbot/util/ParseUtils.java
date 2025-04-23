package dev.rubasace.linkedin.games.ldrbot.util;

import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParseUtils {

    private static final Pattern TIMER_PATTERN = Pattern.compile("\\b(\\d{1,2}):?(\\d{2})\\b");

    public static Optional<Duration> parseDuration(final String durationText) {
        Matcher matcher = TIMER_PATTERN.matcher(durationText);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        int minutes = Integer.parseInt(matcher.group(1));
        int seconds = Integer.parseInt(matcher.group(2));
        return Optional.of(Duration.ofMinutes(minutes).plusSeconds(seconds));
    }
}
