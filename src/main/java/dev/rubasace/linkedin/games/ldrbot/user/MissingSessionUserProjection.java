package dev.rubasace.linkedin.games.ldrbot.user;

import java.time.ZoneId;

public interface MissingSessionUserProjection {
    Long getChatId();

    String getGroupName();

    Long getUserId();
    String getUserName();

    String getFirstName();

    String getLastName();

    ZoneId getTimeZone();
}