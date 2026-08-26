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
     * Everything a result line requires from the ordinal marker onwards.
     * <p>
     * Both patterns below are built from this, so the marker set is written down
     * exactly once and the pre-check can never drift into being stricter than the
     * pattern it guards.
     */
    private static final String RESULT_LINE_TAIL =
            "(?:#|n\\.º)" + SPACE_LIKE + "*+(\\d++)"
                          + SPACE_LIKE + "*(?:\\||\\n)"
                          + SPACE_LIKE + "*+(\\d{1,2}:\\d{2})";

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
     * <p>
     * The space-like runs are possessive everywhere they are followed by something
     * that cannot itself be space-like, which costs nothing and prunes doomed
     * search paths. The run before {@code (?:\||\n)} is deliberately NOT
     * possessive: in the multi-line shape it must give the newline back so the
     * separator can consume it.
     */
    private static final String RESULT_MESSAGE_FORMAT = "^(.+?)" + SPACE_LIKE + "++" + RESULT_LINE_TAIL;

    private static final Pattern MESSAGE_PATTERN = Pattern.compile(RESULT_MESSAGE_FORMAT, Pattern.MULTILINE | Pattern.DOTALL);

    /**
     * Linear-time necessary condition for {@link #MESSAGE_PATTERN}: any character,
     * one space-like character, then the result-line tail.
     * <p>
     * {@link #MESSAGE_PATTERN} opens with a lazy {@code (.+?)} under {@code DOTALL},
     * which backtracks catastrophically on long input that can never match: a
     * 4096-character message of newlines cost minutes of CPU, and any group member
     * can send one. Checking this first means the backtracking pattern only ever
     * sees input that could actually match.
     * <p>
     * Needs {@code DOTALL} so its leading {@code .} can match a newline, and must
     * not have {@code MULTILINE}.
     */
    private static final Pattern RESULT_LINE_PRECHECK = Pattern.compile("." + SPACE_LIKE + RESULT_LINE_TAIL, Pattern.DOTALL);


    public Optional<GameDuration> extractGameDuration(final String message) {
        if (message == null || message.isEmpty()) {
            return Optional.empty();
        }

        if (!RESULT_LINE_PRECHECK.matcher(message).find()) {
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
