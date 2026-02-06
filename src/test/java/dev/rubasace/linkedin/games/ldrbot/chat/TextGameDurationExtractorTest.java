package dev.rubasace.linkedin.games.ldrbot.chat;

import dev.rubasace.linkedin.games.ldrbot.session.GameDuration;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.text.TextGameDurationExtractor;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TextGameDurationExtractorTest {

    private final TextGameDurationExtractor extractor = new TextGameDurationExtractor();

    @Test
    void shouldExtractGameNameAndDurationFromTangoMessage() {
        String message = "Tango #487 | 0:43 🌗";
        Optional<GameDuration> result = extractor.extractGameDuration(message);

        assertTrue(result.isPresent());
        assertEquals(GameType.TANGO, result.get().type());
        assertEquals(Duration.ofMinutes(0).plusSeconds(43), result.get().duration());
    }

    @Test
    void shouldExtractGameNameAndDurationFromZipMessage() {
        String message = "Zip #326 | 0:12 🏁\nWith 1 backtrack 🛑\nlnkd.in/zip.";
        Optional<GameDuration> result = extractor.extractGameDuration(message);

        assertTrue(result.isPresent());
        assertEquals(GameType.ZIP, result.get().type());
        assertEquals(Duration.ofMinutes(0).plusSeconds(12), result.get().duration());
    }

    @Test
    void shouldExtractGameNameAndDurationFromZipMessageWithMultipleLines() {
        String message = "Zip #326 | 0:12 🏁\n\nlnkd.in/zip.\n\n\nConecta los números, completa el tablero y desafía al cronómetro. Juega a diario en lnkd.in/zip!";
        Optional<GameDuration> result = extractor.extractGameDuration(message);

        assertTrue(result.isPresent());
        assertEquals(GameType.ZIP, result.get().type());
        assertEquals(Duration.ofMinutes(0).plusSeconds(12), result.get().duration());
    }

    @Test
    void shouldExtractGameNameAndDurationFromQueensMessage() {
        String message = "Queens #553 | 0:18 👑\n🏅 I'm on a 485-day win streak!\nlnkd.in/queens";
        Optional<GameDuration> result = extractor.extractGameDuration(message);

        assertTrue(result.isPresent());
        assertEquals(GameType.QUEENS, result.get().type());
        assertEquals(Duration.ofMinutes(0).plusSeconds(18), result.get().duration());
    }

    @Test
    void shouldExtractGameNameAndDurationFromMiniSudokuMessage() {
        String message = "Mini Sudoku #100 | 1:23 ⏱️";
        Optional<GameDuration> result = extractor.extractGameDuration(message);

        assertTrue(result.isPresent());
        assertEquals(GameType.SUDOKU, result.get().type());
        assertEquals(Duration.ofMinutes(1).plusSeconds(23), result.get().duration());
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

    @Test
    void shouldReturnEmptyIfWrongFormat() {
        String message = "Yesterday a friend did Queens very fast\nI think it was 0:04\nIt's crazy";
        Optional<GameDuration> result = extractor.extractGameDuration(message);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyForUnknownGame() {
        String message = "UnknownGame #100 | 1:23";
        Optional<GameDuration> result = extractor.extractGameDuration(message);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyForNullMessage() {
        Optional<GameDuration> result = extractor.extractGameDuration(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyForEmptyMessage() {
        Optional<GameDuration> result = extractor.extractGameDuration("");

        assertTrue(result.isEmpty());
    }
}
