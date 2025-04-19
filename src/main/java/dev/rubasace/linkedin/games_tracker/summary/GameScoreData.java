package dev.rubasace.linkedin.games_tracker.summary;

import dev.rubasace.linkedin.games_tracker.session.GameType;

import java.time.Duration;

public record GameScoreData(
        String username,
        GameType game,
        Duration duration,
        int points
) {
}
