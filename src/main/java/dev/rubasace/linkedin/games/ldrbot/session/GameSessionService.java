package dev.rubasace.linkedin.games.ldrbot.session;

import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.group.GroupNotFoundException;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroup;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupService;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUser;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUserService;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Transactional(readOnly = true)
@Service
public class GameSessionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameSessionService.class);

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
        recordGameSession(chatInfo, userInfo, gameDuration, gameDay, Instant.now(), false);
    }

    @Transactional
    public void recordGameSession(final ChatInfo chatInfo, final UserInfo userInfo, final GameDuration gameDuration, final LocalDate gameDay, final boolean allowOverride) throws SessionAlreadyRegisteredException, GroupNotFoundException {
        recordGameSession(chatInfo, userInfo, gameDuration, gameDay, Instant.now(), allowOverride);
    }

    @Transactional
    public void recordGameSession(final ChatInfo chatInfo, final UserInfo userInfo, final GameDuration gameDuration, final LocalDate gameDay, final Instant messageTimestamp, final boolean allowOverride) throws SessionAlreadyRegisteredException, GroupNotFoundException {
        TelegramGroup telegramGroup = telegramGroupService.findGroupOrThrow(chatInfo);
        TelegramUser telegramUser = telegramUserService.findOrCreate(userInfo);
        if (!telegramGroup.getTrackedGames().contains(gameDuration.type())) {
            return;
        }
        GameInfo gameInfo = gameTypeAdapter.adapt(gameDuration.type());
        Optional<GameSession> existingSession = gameSessionRepository.getByUserIdAndGroupChatIdAndGameAndGameDay(telegramUser.getId(), telegramGroup.getChatId(),
                                                                                                                 gameDuration.type(), gameDay);
        // Both captured before any mutation: existingSession.get() is a managed entity, so once setDuration runs the
        // pre-override duration is unrecoverable and isNew can no longer be told apart from an override.
        boolean isNew = existingSession.isEmpty();
        Duration previousOwnDuration = existingSession.map(GameSession::getDuration).orElse(null);
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
            gameSession.setRegisteredAt(messageTimestamp);
        }

        saveSession(chatInfo, userInfo, gameDay, gameSession, gameInfo, telegramGroup, isNew, previousOwnDuration);
    }

    private void saveSession(final ChatInfo chatInfo, final UserInfo userInfo, final LocalDate gameDay, final GameSession gameSession, final GameInfo gameInfo, final TelegramGroup telegramGroup, final boolean isNew, final Duration previousOwnDuration) {
        gameSessionRepository.saveAndFlush(gameSession);
        applicationEventPublisher.publishEvent(new GameSessionRegistrationEvent(this, chatInfo, userInfo, gameInfo, gameSession.getDuration(), gameDay,
                                                                                telegramGroup.getChatId()));

        Long chatId = telegramGroup.getChatId();
        GameType game = gameSession.getGame();
        Duration duration = gameSession.getDuration();
        try {
            boolean improvedOwnRow = isNew || duration.compareTo(previousOwnDuration) < 0;
            if (!improvedOwnRow) {
                return;
            }
            // previousOwnDuration only ever gates: the value announced is always the best of the OTHER rows.
            Optional<Duration> bestOtherDuration = gameSessionRepository.getTop1ByGroupChatIdAndGameAndIdNotOrderByDurationAsc(chatId, game, gameSession.getId())
                                                                        .map(GameSession::getDuration);
            boolean betterThanEveryOther = bestOtherDuration.isEmpty() || duration.compareTo(bestOtherDuration.get()) < 0;
            if (!betterThanEveryOther) {
                return;
            }
            applicationEventPublisher.publishEvent(new GameRecordEstablishedEvent(this, chatInfo, userInfo, gameInfo, duration, bestOtherDuration.orElse(null)));
        } catch (Exception e) {
            LOGGER.warn("Record detection failed for chat {} game {}", chatId, game, e);
        }
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
        return gameSessionRepository.getByGroupUuidAndUserIdInOrderByGameDayDescRegisteredAtDesc(uuid, userIds);
    }

}
