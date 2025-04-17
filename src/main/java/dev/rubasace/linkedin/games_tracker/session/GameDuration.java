package dev.rubasace.linkedin.games_tracker.session;


import java.time.Duration;

//TODO think of adding gameNumber
public record GameDuration(GameType type, Duration duration) {
}
