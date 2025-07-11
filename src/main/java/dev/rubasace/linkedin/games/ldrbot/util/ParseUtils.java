package dev.rubasace.linkedin.games.ldrbot.util;

import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParseUtils {

    private static final Pattern TIMER_PATTERN = Pattern.compile("\\b(\\d{1,2}):?(\\d{2})\\b");
    private static final Pattern ALTERNATIVE_TIMER_PATTERN = Pattern.compile("[a-zA-Z\\s]*"+TIMER_PATTERN);

    public static Optional<Duration> parseIsolatedDuration(final String durationText) {
        return getDuration(durationText, TIMER_PATTERN);
    }

    public static Optional<Duration> parseDurationFromMessage(final String durationText) {
        return getDuration(durationText, ALTERNATIVE_TIMER_PATTERN);
    }

    private static Optional<Duration> getDuration(final String durationText, Pattern pattern) {
        Matcher matcher = pattern.matcher(durationText);

        if (!matcher.matches()) {
            return Optional.empty();
        }

        int minutes = Integer.parseInt(matcher.group(1));
        int seconds = Integer.parseInt(matcher.group(2));
        return Optional.of(Duration.ofMinutes(minutes).plusSeconds(seconds));
    }
}
