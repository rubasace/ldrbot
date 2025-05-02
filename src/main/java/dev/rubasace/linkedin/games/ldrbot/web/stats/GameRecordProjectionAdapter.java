package dev.rubasace.linkedin.games.ldrbot.web.stats;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class GameRecordProjectionAdapter {

    GameRecord adapt(GameSessionProjection gameSessionProjection) {
        return new GameRecord(
                StringUtils.capitalize(gameSessionProjection.getGame().name().toLowerCase()),
                Math.toIntExact(gameSessionProjection.getDuration().toSeconds()),
                gameSessionProjection.getUserId(),
                gameSessionProjection.getUsername(),
                gameSessionProjection.getFirstName(),
                gameSessionProjection.getDate()
        );
    }
}
