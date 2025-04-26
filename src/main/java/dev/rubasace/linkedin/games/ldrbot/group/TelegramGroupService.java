package dev.rubasace.linkedin.games.ldrbot.group;

import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUser;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUserService;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Transactional(readOnly = true)
@Service
public class TelegramGroupService {

    private final TelegramGroupRepository telegramGroupRepository;
    private final TelegramUserService telegramUserService;
    private final ApplicationEventPublisher applicationEventPublisher;

    TelegramGroupService(final TelegramGroupRepository telegramGroupRepository, final TelegramUserService telegramUserService, final ApplicationEventPublisher applicationEventPublisher) {
        this.telegramGroupRepository = telegramGroupRepository;
        this.telegramUserService = telegramUserService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public Optional<TelegramGroup> findGroup(final Long chatId) {
        return telegramGroupRepository.findById(chatId);
    }

    public TelegramGroup findGroupOrThrow(final ChatInfo chatInfo) throws GroupNotFoundException {
        return this.findGroup(chatInfo.chatId()).orElseThrow(() -> new GroupNotFoundException(chatInfo));
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

    //TODO make sure recorded games and scores of the day are deleted for the user
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

    public Stream<TelegramGroup> findGroupsWithMissingScores(final LocalDate gameDay) {
        return telegramGroupRepository.findGroupsWithMissingScores(gameDay);
    }

    private TelegramGroup udpateGroupData(final TelegramGroup telegramGroup, final ChatInfo chatInfo) {
        if (telegramGroup.getGroupName().equals(chatInfo.title())) {
            return telegramGroup;
        }
        telegramGroup.setGroupName(chatInfo.title());
        return telegramGroupRepository.save(telegramGroup);
    }


    private TelegramGroup createGroup(final ChatInfo chatInfo) {
        //TODO stop hardcoding the timezone and request it as part of the command interaction
        TelegramGroup telegramGroup = new TelegramGroup(chatInfo.chatId(), chatInfo.title(), ZoneId.of("Europe/Madrid"), EnumSet.allOf(GameType.class), new HashSet<>(),
                                                        Set.of());
        TelegramGroup createdTelegramGroup = telegramGroupRepository.save(telegramGroup);
        applicationEventPublisher.publishEvent(new GroupCreatedEvent(this, chatInfo));
        return createdTelegramGroup;
    }

    public Set<GameType> listTrackedGames(final ChatInfo chatInfo) throws GroupNotFoundException {
        TelegramGroup telegramGroup = findGroupOrThrow(chatInfo);
        return telegramGroup.getTrackedGames();
    }

    @Transactional
    public void removeGroup(final ChatInfo chatInfo) {
        //TODO allow to remove groups, probably with soft deletion
        //        telegramGroupRepository.deleteById(chatId);
    }

}
