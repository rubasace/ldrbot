package dev.rubasace.linkedin.games_tracker.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class LinkedinTimeUtils {

    //Games are released at midnight pacific time, so we use that ZoneId to set the day
    private static final ZoneId PACIFIC_ZONE_ID = ZoneId.of("America/Los_Angeles");

    public static LocalDate todayGameDay() {
        return ZonedDateTime.now(PACIFIC_ZONE_ID).toLocalDate();
    }
}
