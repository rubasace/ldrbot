package dev.rubasace.linkedin.games.ldrbot.web.session;

import dev.rubasace.linkedin.games.ldrbot.session.GameSession;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.ZonedDateTime;

@Component
class GameSessionAdapter {

    Session adapt(GameSession gameSession) {
        ZonedDateTime registeredAt = gameSession.getRegisteredAt() != null ? gameSession.getRegisteredAt().atZone(gameSession.getGroup().getTimezone()) : null;
        return new Session(gameSession.getUser().getId(),
                           gameSession.getUser().getUserName(),
                           gameSession.getUser().getFirstName(),
                           StringUtils.capitalize(gameSession.getGame().name().toLowerCase()),
                           (int) gameSession.getDuration().toSeconds(),
                           gameSession.getGameDay(),
                           registeredAt);
    }
}
