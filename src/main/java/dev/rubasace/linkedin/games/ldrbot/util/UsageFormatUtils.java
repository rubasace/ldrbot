package dev.rubasace.linkedin.games.ldrbot.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UsageFormatUtils {

    private static final String USAGE_PREFIX = "Usage: ";
    private static final String USAGE_POSTFIX = " - ";
    private static final Pattern USAGE_PATTERN = Pattern.compile("^" + USAGE_PREFIX + "(.*?)" + USAGE_POSTFIX + "(.*)$");

    public static String formatUsage(final String usage, final String description) {
        return USAGE_PREFIX + usage + USAGE_POSTFIX + description;
    }

    public static Optional<String> extractUsage(final String text) {
        final Matcher matcher = USAGE_PATTERN.matcher(text.trim());
        if (matcher.matches()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    public static String extractDescription(final String text) {
        final Matcher matcher = USAGE_PATTERN.matcher(text.trim());
        if (matcher.matches()) {
            return matcher.group(2);
        }
        return text;
    }
}
