package dev.rubasace.linkedin.games.ldrbot.web.stats;

import dev.rubasace.linkedin.games.ldrbot.session.GameType;

import java.time.Duration;
import java.time.LocalDate;

public interface GameSessionProjection {
    GameType getGame();

    Duration getDuration();

    Long getUserId();

    String getUsername();

    String getFirstName();

    LocalDate getDate();
}