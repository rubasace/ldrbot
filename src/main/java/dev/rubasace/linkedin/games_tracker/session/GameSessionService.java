package dev.rubasace.linkedin.games_tracker.session;

import dev.rubasace.linkedin.games_tracker.user.TelegramUser;
import dev.rubasace.linkedin.games_tracker.user.TelegramUserService;
import dev.rubasace.linkedin.games_tracker.util.LinkedinTimeUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Stream;

@Transactional(readOnly = true)
@Service
public class GameSessionService {



    private final GameSessionRepository gameSessionRepository;
    private final TelegramUserService telegramUserService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public GameSessionService(final GameSessionRepository gameSessionRepository, final TelegramUserService telegramUserService, final ApplicationEventPublisher applicationEventPublisher) {
        this.gameSessionRepository = gameSessionRepository;
        this.telegramUserService = telegramUserService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public GameSession recordGameSession(final Long userId, final String userName, final GameDuration gameDuration) throws AlreadyRegisteredSession {
        TelegramUser telegramUser = telegramUserService.findOrCreate(userId, userName);
        LocalDate gameDay = LinkedinTimeUtils.todayGameDay();
        if (gameSessionRepository.existsByUserIdAndGameAndGameDay(telegramUser.getId(), gameDuration.type(), gameDay)) {
            throw new AlreadyRegisteredSession(userName, gameDuration.type());
        }
        GameSession gameSession = new GameSession();
        gameSession.setGame(gameDuration.type());
        gameSession.setUser(telegramUser);
        gameSession.setGameDay(gameDay);
        gameSession.setDuration(gameDuration.duration());
        GameSession savedSession = gameSessionRepository.save(gameSession);
        //TODO think of reacting to this event to notify on the channels when a new time is registered?
        applicationEventPublisher.publishEvent(new GameSessionRegistrationEvent(this, userId));
        return savedSession;
    }


    @Transactional
    public void deleteTodaySession(final Long userId, final GameType game) {
        gameSessionRepository.deleteByUserIdAndGameAndGameDay(userId, game, LinkedinTimeUtils.todayGameDay());
    }

    public Stream<GameSession> getTodaySessions(final Long userId) {
        return gameSessionRepository.getByUserIdAndGameDay(userId, LinkedinTimeUtils.todayGameDay());
    }

    public Stream<GameSession> getTodaySessions(final Set<Long> userIds) {
        return gameSessionRepository.getByUserIdInAndGameDay(userIds, LinkedinTimeUtils.todayGameDay());
    }


    @Transactional
    public void deleteTodaySessions(final Long userId) {
        gameSessionRepository.deleteByUserIdAndGameDay(userId, LinkedinTimeUtils.todayGameDay());
    }


}
