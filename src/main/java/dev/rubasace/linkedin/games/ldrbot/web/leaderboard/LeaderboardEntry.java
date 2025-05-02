package dev.rubasace.linkedin.games.ldrbot.web.leaderboard;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;

public interface LeaderboardEntry {

    int getTotalPoints();

    @JsonIgnore
    Duration getTotalDuration();

    int getTotalGames();

    @JsonProperty("totalDuration")
    default long getTotalDurationSeconds() {
        return getTotalDuration() != null ? getTotalDuration().getSeconds() : 0;
    }
}
