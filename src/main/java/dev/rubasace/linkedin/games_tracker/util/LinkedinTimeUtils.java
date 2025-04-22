package dev.rubasace.linkedin.games_tracker.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class LinkedinTimeUtils {

    public static final String LINKEDIN_ZONE = "America/Los_Angeles";
    //Games are released at midnight pacific time, so we use that ZoneId to set the day
    private static final ZoneId PACIFIC_ZONE_ID = ZoneId.of(LINKEDIN_ZONE);

    /**
     * Convenience method to return the current game day, as games last 24h and get published at midnight, Los Angeles timezone
     */
    public static LocalDate todayGameDay() {
        return ZonedDateTime.now(PACIFIC_ZONE_ID).toLocalDate();
    }
}
