package dev.rubasace.linkedin.games_tracker.summary;

import dev.rubasace.linkedin.games_tracker.session.GameType;

import java.time.Duration;

public record GameScoreData(
        String userName,
        GameType game,
        Duration duration,
        int position,
        int points
) {
}
