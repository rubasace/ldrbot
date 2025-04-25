package dev.rubasace.linkedin.games.ldrbot.summary;

import dev.rubasace.linkedin.games.ldrbot.group.GroupInfo;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;

import java.time.Duration;
import java.util.Objects;

public record GameScoreData(
        GroupInfo groupInfo,
        UserInfo userInfo,
        GameType game,
        Duration duration,
        int position,
        int points
) {

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GameScoreData that = (GameScoreData) o;
        return game == that.game && Objects.equals(userInfo, that.userInfo) && Objects.equals(groupInfo, that.groupInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userInfo, groupInfo, game);
    }
}
