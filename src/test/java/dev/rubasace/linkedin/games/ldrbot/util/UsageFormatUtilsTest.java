package dev.rubasace.linkedin.games.ldrbot.util;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UsageFormatUtilsTest {

    @Test
    void shouldFormatUsage() {
        String usage = "/mycommand <arg1> <arg2>";
        String description = "Does some things";

        String formattedUsage = UsageFormatUtils.formatUsage(usage, description);

        assertEquals("Usage: " + usage + " - " + description, formattedUsage);
    }

    @Test
    void shouldExtractUsageBack() {
        String usage = "/other <arg1>";
        String description = "Does some other things";

        String formattedUsage = UsageFormatUtils.formatUsage(usage, description);
        Optional<String> resultUsage = UsageFormatUtils.extractUsage(formattedUsage);

        assertEquals(Optional.of(usage), resultUsage);
    }


    @Test
    void shouldExtractDescriptionBack() {
        String usage = "/desc <arg1>";
        String description = "Does some more things";

        String formattedUsage = UsageFormatUtils.formatUsage(usage, description);
        String resultDescription = UsageFormatUtils.extractDescription(formattedUsage);

        assertEquals(description, resultDescription);
    }


    @Test
    void shouldNotFailIfDescriptionContainsUsageSeparator() {
        String usage = "/tricky <arg1>";
        String description = "Does some other things - in a tricky way";

        String formattedUsage = UsageFormatUtils.formatUsage(usage, description);
        Optional<String> resultUsage = UsageFormatUtils.extractUsage(formattedUsage);

        assertEquals(Optional.of(usage), resultUsage);
    }

    @Test
    void shouldReturnEmptyAsUsageIfNoUsage() {
        String description = "Does some other things";

        Optional<String> resultUsage = UsageFormatUtils.extractUsage(description);

        assertEquals(Optional.empty(), resultUsage);
    }

    @Test
    void shouldReturnEWholeStringAsDescriptionIfNoUsage() {
        String description = "Does some other things";

        String resultDescription = UsageFormatUtils.extractDescription(description);

        assertEquals(description, resultDescription);
    }

}