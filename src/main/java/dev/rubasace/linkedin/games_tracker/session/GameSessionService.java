package dev.rubasace.linkedin.games_tracker.session;

import dev.rubasace.linkedin.games_tracker.group.GroupNotFoundException;
import dev.rubasace.linkedin.games_tracker.group.TelegramGroup;
import dev.rubasace.linkedin.games_tracker.group.TelegramGroupService;
import dev.rubasace.linkedin.games_tracker.user.TelegramUser;
import dev.rubasace.linkedin.games_tracker.user.TelegramUserService;
import dev.rubasace.linkedin.games_tracker.util.LinkedinTimeUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Transactional(readOnly = true)
@Service
public class GameSessionService {

    private final GameSessionRepository gameSessionRepository;
    private final TelegramUserService telegramUserService;
    private final TelegramGroupService telegramGroupService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public GameSessionService(final GameSessionRepository gameSessionRepository, final TelegramUserService telegramUserService, final TelegramGroupService telegramGroupService, final ApplicationEventPublisher applicationEventPublisher) {
        this.gameSessionRepository = gameSessionRepository;
        this.telegramUserService = telegramUserService;
        this.telegramGroupService = telegramGroupService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public Optional<GameSession> recordGameSession(final Long userId, final Long chatId, final String userName, final GameDuration gameDuration) throws AlreadyRegisteredSession, GroupNotFoundException {
        TelegramGroup telegramGroup = telegramGroupService.findGroupOrThrow(chatId);
        if (!telegramGroup.getTrackedGames().contains(gameDuration.type())) {
            return Optional.empty();
        }
        TelegramUser telegramUser = telegramUserService.findOrCreate(userId, userName);
        LocalDate gameDay = LinkedinTimeUtils.todayGameDay();
        if (gameSessionRepository.existsByUserIdAndGroupChatIdAndGameAndGameDay(userId, chatId, gameDuration.type(), gameDay)) {
            throw new AlreadyRegisteredSession(userName, gameDuration.type(), chatId);
        }
        GameSession gameSession = new GameSession();
        gameSession.setGame(gameDuration.type());
        gameSession.setUser(telegramUser);
        gameSession.setGroup(telegramGroup);
        gameSession.setGameDay(gameDay);
        gameSession.setDuration(gameDuration.duration());
        GameSession savedSession = gameSessionRepository.save(gameSession);
        //TODO think of reacting to this event to notify on the channels when a new time is registered
        applicationEventPublisher.publishEvent(new GameSessionRegistrationEvent(this, userId, chatId));
        return Optional.of(savedSession);
    }

    @Transactional
    public void deleteTodaySession(final Long userId, final Long chatId, final GameType game) {
        gameSessionRepository.deleteByUserIdAndGroupChatIdAndGameAndGameDay(userId, chatId, game, LinkedinTimeUtils.todayGameDay());
    }

    public Stream<GameSession> getTodaySessions(final Long userId, final Long chatId) {
        return gameSessionRepository.getByUserIdAndGroupChatIdAndGameDay(userId, chatId, LinkedinTimeUtils.todayGameDay());
    }

    public Stream<GameSession> getTodaySessions(final Set<Long> userIds, final Long chatId) {
        return gameSessionRepository.getByUserIdInAndGroupChatIdAndGameDay(userIds, chatId, LinkedinTimeUtils.todayGameDay());
    }


    @Transactional
    public void deleteTodaySessions(final Long userId, final Long chatId) {
        gameSessionRepository.deleteByUserIdAndGroupChatIdAndGameDay(userId, chatId, LinkedinTimeUtils.todayGameDay());
    }


}
