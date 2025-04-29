package dev.rubasace.linkedin.games.ldrbot.group;

import dev.rubasace.linkedin.games.ldrbot.session.GameInfo;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.session.GameTypeAdapter;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUser;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUserService;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Transactional(readOnly = true)
@Service
public class TelegramGroupService {

    private final TelegramGroupRepository telegramGroupRepository;
    private final TelegramUserService telegramUserService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final GameTypeAdapter gameTypeAdapter;

    TelegramGroupService(final TelegramGroupRepository telegramGroupRepository, final TelegramUserService telegramUserService, final ApplicationEventPublisher applicationEventPublisher, final GameTypeAdapter gameTypeAdapter) {
        this.telegramGroupRepository = telegramGroupRepository;
        this.telegramUserService = telegramUserService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.gameTypeAdapter = gameTypeAdapter;
    }

    public Optional<TelegramGroup> findGroup(final Long chatId) {
        return telegramGroupRepository.findById(chatId);
    }

    public TelegramGroup findGroupOrThrow(final ChatInfo chatInfo) throws GroupNotFoundException {
        return this.findGroup(chatInfo.chatId()).orElseThrow(() -> new GroupNotFoundException(chatInfo));
    }

    private TelegramGroup findGroupOrThrow(final Long chatId) throws GroupNotFoundException {
        return findGroupOrThrow(new ChatInfo(chatId, null, true));
    }



    @Transactional
    public TelegramGroup registerOrUpdateGroup(final ChatInfo chatInfo) {
        return telegramGroupRepository.findById(chatInfo.chatId())
                                      .map(telegramGroup -> udpateGroupData(telegramGroup, chatInfo))
                                      .orElseGet(() -> this.createGroup(chatInfo));
    }

    @Transactional
    public void addUserToGroup(final ChatInfo chatInfo, final UserInfo userInfo) throws GroupNotFoundException {
        TelegramGroup telegramGroup = findGroupOrThrow(chatInfo);

        TelegramUser telegramUser = telegramUserService.findOrCreate(userInfo);
        if (telegramGroup.getMembers().contains(telegramUser)) {
            return;
        }
        telegramGroup.getMembers().add(telegramUser);
        telegramGroupRepository.save(telegramGroup);
        applicationEventPublisher.publishEvent(new UserJoinedGroupEvent(this, userInfo, chatInfo));
    }

    @Transactional
    public void removeUserFromGroup(final ChatInfo chatInfo, final UserInfo userInfo) throws GroupNotFoundException {
        TelegramGroup telegramGroup = findGroupOrThrow(chatInfo);

        Optional<TelegramUser> telegramUser = telegramUserService.find(userInfo);
        if (telegramUser.isEmpty() || !telegramGroup.getMembers().contains(telegramUser.get())) {
            return;
        }
        telegramGroup.getMembers().remove(telegramUser.get());
        telegramGroupRepository.save(telegramGroup);
        applicationEventPublisher.publishEvent(new UserLeftGroupEvent(this, chatInfo, userInfo));
    }

    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public Stream<TelegramGroup> findGroupsWithMissingScores(final LocalDate gameDay) {
        return telegramGroupRepository.findGroupsWithMissingScores(gameDay);
    }

    private TelegramGroup udpateGroupData(final TelegramGroup telegramGroup, final ChatInfo chatInfo) {
        boolean active = telegramGroup.isActive();
        if (active && telegramGroup.getGroupName().equals(chatInfo.title())) {
            return telegramGroup;
        }
        telegramGroup.setGroupName(chatInfo.title());
        telegramGroup.setActive(true);
        TelegramGroup updatedGroup = telegramGroupRepository.save(telegramGroup);
        if (!active) {
            applicationEventPublisher.publishEvent(new GroupCreatedEvent(this, chatInfo));
        }
        return updatedGroup;
    }


    private TelegramGroup createGroup(final ChatInfo chatInfo) {
        TelegramGroup telegramGroup = new TelegramGroup(chatInfo.chatId(), chatInfo.title());
        TelegramGroup createdTelegramGroup = telegramGroupRepository.save(telegramGroup);
        applicationEventPublisher.publishEvent(new GroupCreatedEvent(this, chatInfo));
        return createdTelegramGroup;
    }

    public Set<GameType> listTrackedGames(final ChatInfo chatInfo) throws GroupNotFoundException {
        TelegramGroup telegramGroup = findGroupOrThrow(chatInfo);
        return telegramGroup.getTrackedGames();
    }

    public Set<GameType> listTrackedGames(final Long chatId) throws GroupNotFoundException {
        return listTrackedGames(new ChatInfo(chatId, null, true));
    }

    @Transactional
    public void removeGroup(final ChatInfo chatInfo) throws GroupNotFoundException {
        TelegramGroup telegramGroup = findGroupOrThrow(chatInfo);
        telegramGroup.setActive(false);
        telegramGroupRepository.save(telegramGroup);
    }

    @Transactional
    public void toggleGameTracking(final Long chatId, final GameType gameType) throws GroupNotFoundException {
        TelegramGroup telegramGroup = findGroupOrThrow(chatId);
        if (telegramGroup.getTrackedGames().contains(gameType)) {
            telegramGroup.getTrackedGames().remove(gameType);
        } else {
            telegramGroup.getTrackedGames().add(gameType);
        }
        telegramGroupRepository.save(telegramGroup);
        Set<GameInfo> trackedGames = telegramGroup.getTrackedGames().stream()
                                                  .map(gameTypeAdapter::adapt)
                                                  .collect(Collectors.toSet());
        applicationEventPublisher.publishEvent(new TrackedGamesChangedEvent(this, chatId, trackedGames));
    }

    @Transactional
    public void setTimezone(final Long chatId, final String timeZone) throws GroupNotFoundException {
        TelegramGroup telegramGroup = findGroupOrThrow(chatId);
        ZoneId timezone = ZoneId.of(timeZone);
        telegramGroup.setTimezone(timezone);
        telegramGroupRepository.save(telegramGroup);
        applicationEventPublisher.publishEvent(new TimezoneChangedEvent(this, chatId, timezone));
    }
}
