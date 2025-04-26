package dev.rubasace.linkedin.games.ldrbot.util;

import org.springframework.util.StringUtils;

import java.util.Arrays;

public class InputSanitizer {

    public static String[] sanitizeArguments(final String[] arguments) {
        if (arguments == null) {
            return new String[]{};
        }
        return Arrays.stream(arguments)
                     .filter(StringUtils::hasText)
                     .toArray(String[]::new);
    }
}
