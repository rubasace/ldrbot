package dev.rubasace.linkedin.games.ldrbot.session;

import dev.rubasace.linkedin.games.ldrbot.group.GroupInfo;
import dev.rubasace.linkedin.games.ldrbot.group.GroupNotFoundException;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroup;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupService;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUser;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUserService;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import dev.rubasace.linkedin.games.ldrbot.util.LinkedinTimeUtils;
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
    public Optional<GameSession> recordGameSession(final GroupInfo groupInfo, final UserInfo userInfo, final GameDuration gameDuration) throws SessionAlreadyRegisteredException, GroupNotFoundException {
        TelegramGroup telegramGroup = telegramGroupService.findGroupOrThrow(groupInfo);
        TelegramUser telegramUser = telegramUserService.findOrCreate(userInfo);
        if (!telegramGroup.getTrackedGames().contains(gameDuration.type())) {
            return Optional.empty();
        }
        LocalDate gameDay = LinkedinTimeUtils.todayGameDay();
        if (gameSessionRepository.existsByUserIdAndGroupChatIdAndGameAndGameDay(telegramUser.getId(), telegramGroup.getChatId(), gameDuration.type(), gameDay)) {
            throw new SessionAlreadyRegisteredException(groupInfo, userInfo, gameDuration.type());
        }
        GameSession gameSession = new GameSession();
        gameSession.setGame(gameDuration.type());
        gameSession.setUser(telegramUser);
        gameSession.setGroup(telegramGroup);
        gameSession.setGameDay(gameDay);
        gameSession.setDuration(gameDuration.duration());
        GameSession savedSession = gameSessionRepository.saveAndFlush(gameSession);
        applicationEventPublisher.publishEvent(
                new GameSessionRegistrationEvent(this, groupInfo, userInfo, gameSession.getGame(), gameSession.getDuration(), gameDay,
                                                 telegramGroup.getChatId()));
        return Optional.of(savedSession);
    }

    @Transactional
    public void deleteTodaySession(final GroupInfo groupInfo, final UserInfo userInfo, final GameType game) {
        gameSessionRepository.deleteByUserIdAndGroupChatIdAndGameAndGameDay(userInfo.id(), groupInfo.chatId(), game, LinkedinTimeUtils.todayGameDay());
        telegramUserService.find(userInfo).ifPresent(
                user -> applicationEventPublisher.publishEvent(new GameSessionDeletionEvent(this, groupInfo, userInfo, game)));
    }

    public Stream<GameSession> getDaySessions(final GroupInfo groupInfo, final UserInfo userInfo, final LocalDate gameDay) {
        return gameSessionRepository.getByUserIdAndGroupChatIdAndGameDay(userInfo.id(), groupInfo.chatId(), gameDay);
    }

    public Stream<GameSession> getDaySessions(final Set<Long> userIds, final Long chatId, final LocalDate gameDay) {
        return gameSessionRepository.getByUserIdInAndGroupChatIdAndGameDay(userIds, chatId, gameDay);
    }

    @Transactional
    public void deleteDaySessions(final GroupInfo groupInfo, final UserInfo userInfo, final LocalDate gameDay) {
        gameSessionRepository.deleteByUserIdAndGroupChatIdAndGameDay(userInfo.id(), groupInfo.chatId(), gameDay);
        telegramUserService.find(userInfo).ifPresent(
                user -> applicationEventPublisher.publishEvent(new GameSessionDeletionEvent(this, groupInfo, userInfo)));
    }


}
