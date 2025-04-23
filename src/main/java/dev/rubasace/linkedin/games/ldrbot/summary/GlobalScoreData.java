package dev.rubasace.linkedin.games.ldrbot.summary;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.util.Objects;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public final class GlobalScoreData {
    private String userName;
    private Duration totalDuration;
    private int position;
    private int points;

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (GlobalScoreData) obj;
        return Objects.equals(this.userName, that.userName) &&
                Objects.equals(this.totalDuration, that.totalDuration) &&
                this.position == that.position &&
                this.points == that.points;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, totalDuration, position, points);
    }

    @Override
    public String toString() {
        return "GlobalScoreData[" +
                "userName=" + userName + ", " +
                "totalDuration=" + totalDuration + ", " +
                "position=" + position + ", " +
                "points=" + points + ']';
    }

}
