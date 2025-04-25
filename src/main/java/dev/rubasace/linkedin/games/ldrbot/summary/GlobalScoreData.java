package dev.rubasace.linkedin.games.ldrbot.summary;

import dev.rubasace.linkedin.games.ldrbot.group.GroupInfo;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
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
    private UserInfo userInfo;
    private GroupInfo groupInfo;
    private Duration totalDuration;
    private int position;
    private int points;

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GlobalScoreData that = (GlobalScoreData) o;
        return Objects.equals(userInfo, that.userInfo) && Objects.equals(groupInfo, that.groupInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userInfo, groupInfo);
    }
}
