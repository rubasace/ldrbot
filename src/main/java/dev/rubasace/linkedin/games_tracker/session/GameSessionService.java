package dev.rubasace.linkedin.games_tracker.session;

import dev.rubasace.linkedin.games_tracker.user.TelegramUser;
import dev.rubasace.linkedin.games_tracker.user.TelegramUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.UUID;

@Transactional(readOnly = true)
@Service
public class GameSessionService {

    private final GameSessionRepository gameSessionRepository;
    private final TelegramUserService telegramUserService;

    public GameSessionService(final GameSessionRepository gameSessionRepository, final TelegramUserService telegramUserService) {
        this.gameSessionRepository = gameSessionRepository;
        this.telegramUserService = telegramUserService;
    }

    @Transactional
    public GameSession recordGameSession(final User user, final GameDuration gameDuration) {
        TelegramUser telegramUser = telegramUserService.findOrCreate(user);
        GameSession gameSession = new GameSession();
        gameSession.setId(UUID.randomUUID());
        gameSession.setGame(gameDuration.type());
        gameSession.setTelegramUser(telegramUser);
        return gameSessionRepository.save(gameSession);
    }

    public Iterable<GameSession> findAll() {
        return gameSessionRepository.findAll();
    }

}
