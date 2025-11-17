package dev.rubasace.linkedin.games.ldrbot.chat;

import dev.rubasace.linkedin.games.ldrbot.session.GameDuration;
import dev.rubasace.linkedin.games.ldrbot.session.GameName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MessageGameDurationExtractorTest {

    private final MessageGameDurationExtractor extractor = new MessageGameDurationExtractor();

    @Test
    void shouldExtractGameNameAndDuration() {
        String message = "Queens #553\n0:18 👑\n🏅 I’m on a 485-day win streak!\nlnkd.in/queens";
        Optional<GameDuration> result = extractor.extractGameDuration(message);

        assertTrue(result.isPresent());
        assertEquals(GameName.QUEENS.getType(), result.get().type());
        assertEquals(Duration.ofMinutes(0).plusSeconds(18), result.get().duration());
    }

    @Test
    void shouldReturnEmptyIfNoGameName() {
        String message = "No game here 0:18";
        Optional<GameDuration> result = extractor.extractGameDuration(message);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyIfNoDuration() {
        String message = "Queens #553\nNo time here";
        Optional<GameDuration> result = extractor.extractGameDuration(message);

        assertTrue(result.isEmpty());
    }
}
