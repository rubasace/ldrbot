package dev.rubasace.linkedin.games.ldrbot.text;

import dev.rubasace.linkedin.games.ldrbot.session.GameDuration;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
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
    void shouldExtractGameNameAndDurationFromSpanishMessages() {
        String message = """
                Tango n.º 487 | 0:46 y sin fallos
                Primeros 5 movimientos:
                🟨🟨🟨2️⃣🟨🟨
                🟨🟨1️⃣🟨🟨🟨
                🟨4️⃣5️⃣🟨🟨🟨
                🟨🟨🟨🟨🟨🟨
                🟨🟨🟨3️⃣🟨🟨
                🟨🟨🟨🟨🟨🟨
                🏅 ¡Hoy he estado más audaz que el 50 % de los consejeros delegados!""";
        Optional<GameDuration> result = extractor.extractGameDuration(message);

        assertTrue(result.isPresent());
        assertEquals(GameType.TANGO, result.get().type());
        assertEquals(Duration.ofMinutes(0).plusSeconds(46), result.get().duration());
    }

    @Test
    void shouldExtractGameNameAndDurationFromMultiLineFormat() {
        String message = """
                Queens # 647
                0:31 👑
                [lnkd.in/queens](https://lnkd.in/queens).""";
        Optional<GameDuration> result = extractor.extractGameDuration(message);

        assertTrue(result.isPresent());
        assertEquals(GameType.QUEENS, result.get().type());
        assertEquals(Duration.ofMinutes(0).plusSeconds(31), result.get().duration());
    }

    @Test
    void shouldExtractGameNameAndDurationFromSpanishMessagesMultiLineFormat() {
        String message = """
                Queens n.º 647
                0:31 👑
                [lnkd.in/queens](https://lnkd.in/queens).""";
        Optional<GameDuration> result = extractor.extractGameDuration(message);

        assertTrue(result.isPresent());
        assertEquals(GameType.QUEENS, result.get().type());
        assertEquals(Duration.ofMinutes(0).plusSeconds(31), result.get().duration());
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

    // ---------------------------------------------------------------------
    // Issue #35 — LinkedIn's Spanish share templates separate the ordinal
    // marker from the puzzle number with a non-breaking space, which Java's
    // \s does not match. Those results were silently dropped.
    //
    // The separators below are written as unicode escapes ON PURPOSE. A raw
    // U+00A0 pasted into this file is one editor auto-format away from
    // becoming an ordinary space, which would turn these tests into copies of
    // the ASCII-space case that still pass green.
    // ---------------------------------------------------------------------

    /** U+00A0 NO-BREAK SPACE — what Spanish typography puts after "n.º". */
    private static final String NBSP = "\u00A0";
    /** U+202F NARROW NO-BREAK SPACE. */
    private static final String NNBSP = "\u202F";
    /** U+2009 THIN SPACE — present so the fix is a class, not a hand-picked pair. */
    private static final String THIN_SPACE = "\u2009";

    private void assertRecorded(final String message, final GameType expectedType, final int expectedSeconds) {
        Optional<GameDuration> result = extractor.extractGameDuration(message);
        assertTrue(result.isPresent(), () -> "expected a session for: " + describe(message));
        assertEquals(expectedType, result.get().type());
        assertEquals(Duration.ofSeconds(expectedSeconds), result.get().duration());
    }

    private void assertIgnored(final String message) {
        assertTrue(extractor.extractGameDuration(message).isEmpty(),
                   () -> "expected NO session for: " + describe(message));
    }

    /** Renders invisible code points so a failure message is actually readable. */
    private String describe(final String message) {
        StringBuilder out = new StringBuilder();
        message.codePoints().forEach(cp -> {
            if (cp == '\n') out.append("<LF>");
            else if (cp == '\r') out.append("<CR>");
            else if (cp > 127) out.append(String.format("<U+%04X>", cp));
            else out.append((char) cp);
        });
        return out.toString();
    }

    /**
     * Guards the fixtures themselves. Every other test here would still pass if
     * these constants silently degraded into ordinary spaces, so the constants
     * are pinned to their exact code points.
     */
    @Test
    void separatorConstantsAreTheExactCodePointsTheyClaimToBe() {
        assertEquals(1, NBSP.length());
        assertEquals(1, NNBSP.length());
        assertEquals(1, THIN_SPACE.length());
        assertEquals(0x00A0, NBSP.codePointAt(0));
        assertEquals(0x202F, NNBSP.codePointAt(0));
        assertEquals(0x2009, THIN_SPACE.codePointAt(0));
        // The whole defect in one assertion: Java does not consider these whitespace.
        assertFalse(NBSP.matches("\\s"));
        assertFalse(NNBSP.matches("\\s"));
    }

    @Test
    void shouldExtractPatchesResultWithNoBreakSpaceAfterOrdinalMarker() {
        assertRecorded("Patches n.º" + NBSP + "161 | 0:19 🧶\n"
                       + "Sin pistas y 2 reintentos\n"
                       + "🏅 ¡Llevo 160 días de racha ganadora!\n"
                       + "[lnkd.in/patches](https://lnkd.in/patches).", GameType.PATCHES, 19);
    }

    @Test
    void shouldExtractPatchesResultWithNarrowNoBreakSpaceAfterOrdinalMarker() {
        assertRecorded("Patches n.º" + NNBSP + "161 | 0:19 🧶", GameType.PATCHES, 19);
    }

    @Test
    void shouldExtractPatchesResultWithThinSpaceAfterOrdinalMarker() {
        assertRecorded("Patches n.º" + THIN_SPACE + "161 | 0:19 🧶", GameType.PATCHES, 19);
    }

    @Test
    void shouldTolerateNoBreakSpaceAtEverySeparatorPosition() {
        assertRecorded("Patches" + NBSP + "n.º 161 | 0:19", GameType.PATCHES, 19);
        assertRecorded("Patches" + NBSP + "n.º" + NBSP + "161 | 0:19", GameType.PATCHES, 19);
        assertRecorded("Patches n.º 161" + NBSP + "|" + NBSP + "0:19", GameType.PATCHES, 19);
        assertRecorded("Patches n.º 161 |" + NBSP + "0:19", GameType.PATCHES, 19);
        assertRecorded("Patches n.º 161\n" + NBSP + "0:19", GameType.PATCHES, 19);
    }

    @Test
    void shouldTolerateNoBreakSpaceForEveryTrackedGame() {
        assertRecorded("Queens n.º" + NBSP + "847 | 0:36", GameType.QUEENS, 36);
        assertRecorded("Tango n.º" + NBSP + "487 | 0:46", GameType.TANGO, 46);
        assertRecorded("CrossClimb n.º" + NBSP + "123 | 0:45", GameType.CROSSCLIMB, 45);
        assertRecorded("Mini Sudoku n.º" + NBSP + "100 | 1:23", GameType.SUDOKU, 83);
        assertRecorded("Zip n.º" + NBSP + "326 | 0:12", GameType.ZIP, 12);
        assertRecorded("Patches n.º" + NBSP + "161 | 0:19", GameType.PATCHES, 19);
    }

    @Test
    void shouldTolerateNoBreakSpaceInTheMultiLineShape() {
        assertRecorded("Queens n.º" + NBSP + "647\n0:31 👑\n[lnkd.in/queens](https://lnkd.in/queens).",
                       GameType.QUEENS, 31);
    }

    @Test
    void shouldTolerateNoBreakSpaceAfterTheHashMarker() {
        assertRecorded("Patches #" + NBSP + "161 | 0:19", GameType.PATCHES, 19);
    }

    @Test
    void shouldStillRejectOrdinalMarkerVariantsLinkedInDoesNotEmit() {
        assertIgnored("Patches N.º 161 | 0:19");
        assertIgnored("Patches nº 161 | 0:19");
        assertIgnored("Patches n.° 161 | 0:19");
        assertIgnored("Patches n.o 161 | 0:19");
    }

    /** Deferred to issue #37; pinned here so widening it later is a deliberate act. */
    @Test
    void shouldStillRejectThousandsSeparatedPuzzleNumbers() {
        assertIgnored("Patches n.º 1.161 | 0:19");
        assertIgnored("Patches #1,161 | 0:19");
        assertIgnored("Patches n.º 1" + NNBSP + "161 | 0:19");
    }

    /**
     * Recognition is also the filter that keeps ordinary chat from becoming a
     * score submission, so widening whitespace must not widen that.
     */
    @Test
    void shouldStillRejectOrdinaryChatThatMentionsAGameAndATime() {
        assertIgnored("Ayer jugué Queens n.º 553 y tardé | 0:18 creo");
        assertIgnored("Hoy he hecho el Patches n.º 161 y he tardado | 0:19");
        assertIgnored("Queens is my no. 1 game | 0:18");
        assertIgnored("Fwd from LinkedIn:\nPatches n.º 161 | 0:19");
    }

    @Test
    void shouldStillRequireTheOrdinalMarkerAndThePuzzleNumber() {
        assertIgnored("Patches 161 | 0:19");
        assertIgnored("Patches" + NBSP + "161 | 0:19");
        assertIgnored("Patches n.º | 0:19");
    }

    @Test
    void shouldNotLetSpaceToleranceReachInsideTheTimerOrTheResultShape() {
        assertIgnored("Patches n.º 161 | 0:" + NBSP + "19");
        assertIgnored("Patches n.º 161 🧶 | 0:19");
        assertIgnored("Patches n.º 161 | 100:00");
    }

    /**
     * Mini Sudoku is the only multi-word game name. Tolerance is for separators
     * between the parts of a result line, not for the game name itself.
     */
    @Test
    void shouldNotTolerateANoBreakSpaceInsideAGameName() {
        assertIgnored("Mini" + NBSP + "Sudoku n.º 100 | 1:23");
    }

    /**
     * Widening the whitespace class must be strictly additive: these shapes are
     * recorded today and are not covered by the cases above.
     */
    @Test
    void shouldKeepRecordingShapesThatAlreadyWorkedBeforeTheFix() {
        assertRecorded("Queens #553 |\r\n0:18", GameType.QUEENS, 18);
        assertRecorded("Queens\n\n#553 | 0:18", GameType.QUEENS, 18);
        assertRecorded("Queens\t#553 | 0:18", GameType.QUEENS, 18);
        assertRecorded("Patches n.º 161 | 0:19 🧶", GameType.PATCHES, 19);
    }

    // ---------------------------------------------------------------------
    // Catastrophic backtracking. The pattern opens with a lazy (.+?) under
    // DOTALL, so input that can never match used to make it explore an
    // exponential number of paths. Message text is attacker-controlled: any
    // group member can send ~4.096 characters, and the bot serves every group
    // from one process, so a single stall degrades everyone.
    // ---------------------------------------------------------------------

    /** Telegram's per-message limit, which bounds what an attacker can send. */
    private static final int TELEGRAM_MESSAGE_LIMIT = 4096;

    @Test
    void shouldRejectHostileInputWithoutCatastrophicBacktracking() {
        // Before the pre-check this single input took over four minutes of CPU.
        String bomb = "\n".repeat(TELEGRAM_MESSAGE_LIMIT);
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> assertIgnored(bomb));
    }

    @Test
    void shouldRejectHostileInputThatCarriesAnOrdinalMarker() {
        // A marker-only pre-check would be defeated by exactly this: the marker
        // is present, so the guard passes and the pattern still explodes.
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            assertIgnored("\n".repeat(TELEGRAM_MESSAGE_LIMIT - 1) + "#");
            assertIgnored("\n".repeat(TELEGRAM_MESSAGE_LIMIT - 8) + "#1 | 0:1");
            assertIgnored("Queens" + "\n".repeat(TELEGRAM_MESSAGE_LIMIT - 6));
        });
    }

    @Test
    void shouldRejectHostileInputBuiltFromMixedUnicodeSpaces() {
        // Widening the space class widened what an attacker can build runs from.
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            assertIgnored(("\n" + NBSP).repeat(TELEGRAM_MESSAGE_LIMIT / 2));
            assertIgnored(("\n" + NBSP + " ").repeat(TELEGRAM_MESSAGE_LIMIT / 3));
            assertIgnored("\t".repeat(TELEGRAM_MESSAGE_LIMIT));
        });
    }

    /**
     * The pre-check is a second place recognition can say no, so if it were ever
     * stricter than the pattern it guards, real results would be dropped silently
     * — the very failure this whole change exists to fix. It is built from the
     * same shared sub-pattern, and this pins that property against inputs whose
     * shape makes the two disagree if the flags drift apart.
     */
    @Test
    void preCheckMustNeverRejectAResultThePatternWouldAccept() {
        assertRecorded("Queens\n\n#553 | 0:18", GameType.QUEENS, 18);
        assertRecorded("Queens #553 |\r\n0:18", GameType.QUEENS, 18);
        assertRecorded("\nQueens #553 | 0:18", GameType.QUEENS, 18);
        assertRecorded("Queens n.º" + NBSP + "647\n0:31", GameType.QUEENS, 31);
        assertRecorded("Mini Sudoku # 100\n1:23", GameType.SUDOKU, 83);
    }
}
