package dev.rubasace.linkedin.games.ldrbot.session;

import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.group.GroupNotFoundException;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroup;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupService;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUser;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUserService;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
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
    private final GameTypeAdapter gameTypeAdapter;

    GameSessionService(final GameSessionRepository gameSessionRepository, final TelegramUserService telegramUserService, final TelegramGroupService telegramGroupService, final ApplicationEventPublisher applicationEventPublisher, final GameTypeAdapter gameTypeAdapter) {
        this.gameSessionRepository = gameSessionRepository;
        this.telegramUserService = telegramUserService;
        this.telegramGroupService = telegramGroupService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.gameTypeAdapter = gameTypeAdapter;
    }

    @Transactional
    public void recordGameSession(final ChatInfo chatInfo, final UserInfo userInfo, final GameDuration gameDuration, final LocalDate gameDay) throws SessionAlreadyRegisteredException, GroupNotFoundException {
        recordGameSession(chatInfo, userInfo, gameDuration, gameDay, false);
    }

    @Transactional
    public void recordGameSession(final ChatInfo chatInfo, final UserInfo userInfo, final GameDuration gameDuration, final LocalDate gameDay, final boolean allowOverride) throws SessionAlreadyRegisteredException, GroupNotFoundException {
        TelegramGroup telegramGroup = telegramGroupService.findGroupOrThrow(chatInfo);
        TelegramUser telegramUser = telegramUserService.findOrCreate(userInfo);
        if (!telegramGroup.getTrackedGames().contains(gameDuration.type())) {
            return;
        }
        GameInfo gameInfo = gameTypeAdapter.adapt(gameDuration.type());
        Optional<GameSession> existingSession = gameSessionRepository.getByUserIdAndGroupChatIdAndGameAndGameDay(telegramUser.getId(), telegramGroup.getChatId(),
                                                                                                                 gameDuration.type(), gameDay);
        GameSession gameSession;
        if (existingSession.isPresent()) {
            if (allowOverride) {
                gameSession = existingSession.get();
                gameSession.setDuration(gameDuration.duration());
            } else {
                throw new SessionAlreadyRegisteredException(chatInfo, userInfo, gameInfo);
            }
        } else {
            gameSession = new GameSession();
            gameSession.setGame(gameDuration.type());
            gameSession.setUser(telegramUser);
            gameSession.setGroup(telegramGroup);
            gameSession.setGameDay(gameDay);
            gameSession.setDuration(gameDuration.duration());
        }

        saveSession(chatInfo, userInfo, gameDay, gameSession, gameInfo, telegramGroup);
    }

    private void saveSession(final ChatInfo chatInfo, final UserInfo userInfo, final LocalDate gameDay, final GameSession gameSession, final GameInfo gameInfo, final TelegramGroup telegramGroup) {
        gameSessionRepository.saveAndFlush(gameSession);
        applicationEventPublisher.publishEvent(new GameSessionRegistrationEvent(this, chatInfo, userInfo, gameInfo, gameSession.getDuration(), gameDay,
                                                                                telegramGroup.getChatId()));
    }

    public Optional<GameSession> getDaySession(final ChatInfo chatInfo, final UserInfo userInfo, final GameType gameType, final LocalDate gameDay) {
        return gameSessionRepository.getByUserIdAndGroupChatIdAndGameAndGameDay(userInfo.id(), chatInfo.chatId(), gameType, gameDay);

    }

    @Transactional
    public void deleteDaySession(final ChatInfo chatInfo, final UserInfo userInfo, final GameType gameType, final LocalDate gameDay) {
        gameSessionRepository.deleteByUserIdAndGroupChatIdAndGameAndGameDay(userInfo.id(), chatInfo.chatId(), gameType, gameDay);
        GameInfo gameInfo = gameTypeAdapter.adapt(gameType);
        telegramUserService.find(userInfo).ifPresent(user -> applicationEventPublisher.publishEvent(new GameSessionDeletionEvent(this, chatInfo, userInfo, gameInfo)));
    }

    public Stream<GameSession> getDaySessions(final ChatInfo chatInfo, final UserInfo userInfo, final LocalDate gameDay) {
        return gameSessionRepository.getByUserIdAndGroupChatIdAndGameDay(userInfo.id(), chatInfo.chatId(), gameDay);
    }

    public Stream<GameSession> getDaySessions(final Set<Long> userIds, final Long chatId, final LocalDate gameDay) {
        return gameSessionRepository.getByUserIdInAndGroupChatIdAndGameDay(userIds, chatId, gameDay);
    }

    @Transactional
    public void deleteDaySessions(final ChatInfo chatInfo, final UserInfo userInfo, final LocalDate gameDay) {
        gameSessionRepository.deleteByUserIdAndGroupChatIdAndGameDay(userInfo.id(), chatInfo.chatId(), gameDay);
        telegramUserService.find(userInfo).ifPresent(
                user -> applicationEventPublisher.publishEvent(new GameSessionDeletionEvent(this, chatInfo, userInfo)));
    }

    public Stream<GameSession> getGameSessions(final String uuid, final Set<Long> userIds) {
        return gameSessionRepository.getByGroupUuidAndUserIdInOrderByGameDayDesc(uuid, userIds);
    }

}
