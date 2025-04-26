package dev.rubasace.linkedin.games.ldrbot.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class LinkedinTimeUtils {

    //Games are released at midnight pacific time, so we use that ZoneId to set the day
    public static final String LINKEDIN_ZONE = "America/Los_Angeles";
    private static final ZoneId LINKEDIN_ZONE_ID = ZoneId.of(LINKEDIN_ZONE);

    /**
     * Convenience method to return the current game day, as games last 24h and get published at midnight, Los Angeles timezone
     */
    public static LocalDate todayGameDay() {
        return ZonedDateTime.now(LINKEDIN_ZONE_ID).toLocalDate();
    }
}
