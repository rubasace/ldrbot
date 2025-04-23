package dev.rubasace.linkedin.games.ldrbot.summary;

import dev.rubasace.linkedin.games.ldrbot.session.GameType;

import java.time.Duration;

public record GameScoreData(
        String userName,
        GameType game,
        Duration duration,
        int position,
        int points
) {
}
