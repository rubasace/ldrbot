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

    public TelegramGroup findGroupOrThrow(final GroupInfo groupInfo) throws GroupNotFoundException {
        return this.findGroup(groupInfo.chatId()).orElseThrow(() -> new GroupNotFoundException(groupInfo));
    }


    @Transactional
    public TelegramGroup registerOrUpdateGroup(final GroupInfo groupInfo) {
        return telegramGroupRepository.findById(groupInfo.chatId())
                                      .map(telegramGroup -> udpateGroupData(telegramGroup, groupInfo))
                                      .orElseGet(() -> this.createGroup(groupInfo));
    }

    @Transactional
    public void addUserToGroup(final GroupInfo groupInfo, final UserInfo userInfo) throws GroupNotFoundException {
        TelegramGroup telegramGroup = findGroupOrThrow(groupInfo);

        TelegramUser telegramUser = telegramUserService.findOrCreate(userInfo);
        if (telegramGroup.getMembers().contains(telegramUser)) {
            return;
        }
        telegramGroup.getMembers().add(telegramUser);
        telegramGroupRepository.save(telegramGroup);
        applicationEventPublisher.publishEvent(new UserJoinedGroupEvent(this, userInfo, groupInfo));
    }

    //TODO make sure recorded games and scores of the day are deleted for the user
    @Transactional
    public void removeUserFromGroup(final GroupInfo groupInfo, final UserInfo userInfo) throws GroupNotFoundException {
        TelegramGroup telegramGroup = findGroupOrThrow(groupInfo);

        Optional<TelegramUser> telegramUser = telegramUserService.find(userInfo);
        if (telegramUser.isEmpty() || !telegramGroup.getMembers().contains(telegramUser.get())) {
            return;
        }
        telegramGroup.getMembers().remove(telegramUser.get());
        telegramGroupRepository.save(telegramGroup);
        applicationEventPublisher.publishEvent(new UserLeftGroupEvent(this, groupInfo, userInfo));
    }

    public Stream<TelegramGroup> findGroupsWithMissingScores(final LocalDate gameDay) {
        return telegramGroupRepository.findGroupsWithMissingScores(gameDay);
    }

    private TelegramGroup udpateGroupData(final TelegramGroup telegramGroup, final GroupInfo groupInfo) {
        if (telegramGroup.getGroupName().equals(groupInfo.title())) {
            return telegramGroup;
        }
        telegramGroup.setGroupName(groupInfo.title());
        return telegramGroupRepository.save(telegramGroup);
    }


    private TelegramGroup createGroup(final GroupInfo groupInfo) {
        //TODO stop hardcoding the timezone and request it as part of the command interaction
        TelegramGroup telegramGroup = new TelegramGroup(groupInfo.chatId(), groupInfo.title(), ZoneId.of("Europe/Madrid"), EnumSet.allOf(GameType.class), new HashSet<>(),
                                                        Set.of());
        TelegramGroup createdTelegramGroup = telegramGroupRepository.save(telegramGroup);
        applicationEventPublisher.publishEvent(new GroupCreatedEvent(this, groupInfo));
        return createdTelegramGroup;
    }

    public Set<GameType> listTrackedGames(final GroupInfo groupInfo) throws GroupNotFoundException {
        TelegramGroup telegramGroup = findGroupOrThrow(groupInfo);
        return telegramGroup.getTrackedGames();
    }

    @Transactional
    public void removeGroup(final GroupInfo groupInfo) {
        //TODO allow to remove groups, probably with soft deletion
        //        telegramGroupRepository.deleteById(chatId);
    }

}
