package dev.rubasace.linkedin.games_tracker.summary;

import java.time.Duration;

public record GlobalScoreData(
        String username,
        Duration totalDuration,
        int points
) {
}
