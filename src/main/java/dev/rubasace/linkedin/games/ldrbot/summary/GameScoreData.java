package dev.rubasace.linkedin.games.ldrbot.summary;

import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.session.GameInfo;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;

import java.time.Duration;
import java.util.Objects;

public record GameScoreData(
        ChatInfo chatInfo,
        UserInfo userInfo,
        GameInfo gameInfo,
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
        return gameInfo == that.gameInfo && Objects.equals(userInfo, that.userInfo) && Objects.equals(chatInfo, that.chatInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userInfo, chatInfo, gameInfo);
    }
}
