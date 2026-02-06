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
     */
    private static final String RESULT_MESSAGE_FORMAT = "^(.+?)\\s+(?:#|n\\.º)\\s*(\\d+)\\s*(?:\\||\\n)\\s*(\\d{1,2}:\\d{2}).*";

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

