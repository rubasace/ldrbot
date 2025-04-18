package dev.rubasace.linkedin.games_tracker.session;

import dev.rubasace.linkedin.games_tracker.user.TelegramUser;
import dev.rubasace.linkedin.games_tracker.user.TelegramUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Transactional(readOnly = true)
@Service
public class GameSessionService {

    //Games are released at midnight pacific time, so we use that ZoneId to set the day
    public static final ZoneId PACIFIC_ZONE_ID = ZoneId.of("America/Los_Angeles");

    private final GameSessionRepository gameSessionRepository;
    private final TelegramUserService telegramUserService;

    public GameSessionService(final GameSessionRepository gameSessionRepository, final TelegramUserService telegramUserService) {
        this.gameSessionRepository = gameSessionRepository;
        this.telegramUserService = telegramUserService;
    }

    @Transactional
    public GameSession recordGameSession(final Long userId, final String userName, final GameDuration gameDuration) throws AlreadyRegisteredSession {
        TelegramUser telegramUser = telegramUserService.findOrCreate(userId, userName);
        LocalDate gameDay = todayGameDay();
        if (gameSessionRepository.existsByTelegramUserIdAndGameAndGameDay(telegramUser.getId(), gameDuration.type(), gameDay)) {
            throw new AlreadyRegisteredSession(userName, gameDuration.type());
        }
        GameSession gameSession = new GameSession();
        gameSession.setGame(gameDuration.type());
        gameSession.setTelegramUser(telegramUser);
        gameSession.setGameDay(gameDay);
        gameSession.setDuration(gameDuration.duration());
        return gameSessionRepository.save(gameSession);
    }

    private static LocalDate todayGameDay() {
        return ZonedDateTime.now(PACIFIC_ZONE_ID).toLocalDate();
    }

    public void deleteTodaySession(final Long userId, final GameType game) {
        gameSessionRepository.deleteByTelegramUserIdAndGameAndGameDay(userId, game, todayGameDay());
    }

    public void deleteTodaySessions(final Long userId) {
        gameSessionRepository.deleteByTelegramUserIdAndGameDay(userId, todayGameDay());
    }

}
