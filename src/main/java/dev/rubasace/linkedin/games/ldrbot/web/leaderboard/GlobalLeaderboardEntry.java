package dev.rubasace.linkedin.games.ldrbot.web.leaderboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.Objects;

@AllArgsConstructor
@Setter
@Getter
public final class GlobalLeaderboardEntry implements LeaderboardEntry {
    private final Long userId;
    private final String firstName;
    private final String lastName;
    private final String username;
    private int totalPoints;
    private Duration totalDuration;
    private int totalGames;

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GlobalLeaderboardEntry that = (GlobalLeaderboardEntry) o;
        return Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(userId);
    }
}