package dev.rubasace.linkedin.games.ldrbot.chat;

import dev.rubasace.linkedin.games.ldrbot.session.GameDuration;
import dev.rubasace.linkedin.games.ldrbot.session.GameName;
import dev.rubasace.linkedin.games.ldrbot.util.ParseUtils;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Component
public class MessageGameDurationExtractor {

    public Optional<GameDuration> extractGameDuration(final String message) {
        Optional<GameName> gameName = extractGameNameFromText(message);
        if (gameName.isEmpty()) {
            return Optional.empty();
        }
        Duration duration = extractDurationFromText(message);
        return duration == null ? Optional.empty() : Optional.of(new GameDuration(gameName.get().getType(), duration));
    }

    Optional<GameName> extractGameNameFromText(String message) {
        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains(GameName.QUEENS.getName())) {
            return Optional.of(GameName.QUEENS);
        } else if (lowerMessage.contains(GameName.TANGO.getName())) {
            return Optional.of(GameName.TANGO);
        } else if (lowerMessage.contains(GameName.ZIP.getName())) {
            return Optional.of(GameName.ZIP);
        } else if (lowerMessage.contains(GameName.SUDOKU.getName())) {
            return Optional.of(GameName.SUDOKU);
        }
        return Optional.empty();
    }

    /**
     * Extracts the duration from a text message in the 'official' format:
     * Queens #553\n0:18 👑\n🏅 I’m on a 485-day win streak!\nlnkd.in/queens.
     */
    Duration extractDurationFromText(String message) {
        return Arrays.stream(message.split("\\s+|\\n"))
                .map(token -> ParseUtils.parseIsolatedDuration(token.trim()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElse(null);
    }
}