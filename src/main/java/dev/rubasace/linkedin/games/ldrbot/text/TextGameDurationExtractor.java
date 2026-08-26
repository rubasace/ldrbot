package dev.rubasace.linkedin.games.ldrbot.text;

import dev.rubasace.linkedin.games.ldrbot.session.GameDuration;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.util.ParseUtils;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TextGameDurationExtractor {

    /**
     * ASCII whitespace plus every Unicode space separator (general category Zs).
     * <p>
     * Java's {@code \s} is only {@code [ \t\n\x0B\f\r]}, so it does not match the
     * no-break space (U+00A0) that Spanish typography places between an ordinal
     * abbreviation and its numeral. LinkedIn's Spanish share templates emit one,
     * which made those results go unrecognised entirely (issue #35).
     */
    private static final String SPACE_LIKE = "[\\s\\p{Zs}]";


    /**
     * Pattern to match LinkedIn game messages in the format:
     * <game> #<number> | <mm:ss> [emoji]
     * Example: "Tango #487 | 0:43 🌗"
     * Also matches Spanish format:
     * <game> n.º <number> | <mm:ss> [text]
     * Example: "Tango n.º 487 | 0:46 y sin fallos"
     * Also matches multi-line format:
     * <game> # <number>
     * <mm:ss> [emoji]
     * Example: "Queens # 647\n0:31 👑"
     * <p>
     * Every separator position accepts any {@link #SPACE_LIKE} character, not just
     * ASCII space, so a share whose spacing uses U+00A0 or U+202F is still matched.
     */
    private static final String RESULT_MESSAGE_FORMAT = "^(.+?)" + SPACE_LIKE + "+(?:#|n\\.º)" + SPACE_LIKE + "*(\\d+)" + SPACE_LIKE + "*(?:\\||\\n)" + SPACE_LIKE + "*(\\d{1,2}:\\d{2}).*";

    private static final Pattern MESSAGE_PATTERN = Pattern.compile(RESULT_MESSAGE_FORMAT, Pattern.MULTILINE | Pattern.DOTALL);


    public Optional<GameDuration> extractGameDuration(final String message) {
        if (message == null || message.isEmpty()) {
            return Optional.empty();
        }

        Matcher matcher = MESSAGE_PATTERN.matcher(message);

        if (!matcher.find()) {
            return Optional.empty();
        }

        Optional<GameType> gameType = GameType.fromName(matcher.group(1));
        return gameType.flatMap(type -> ParseUtils.parseIsolatedDuration(matcher.group(3))
                .map(duration -> new GameDuration(type, duration)));

    }
}

