package dev.rubasace.linkedin.games.ldrbot.user;

public interface MissingSessionUserProjection {
    Long getChatId();

    String getGroupName();

    Long getUserId();
    String getUserName();

    String getFirstName();

    String getLastName();
}