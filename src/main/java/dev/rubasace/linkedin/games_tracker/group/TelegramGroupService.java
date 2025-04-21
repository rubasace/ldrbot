package dev.rubasace.linkedin.games_tracker.group;

import dev.rubasace.linkedin.games_tracker.session.GameType;
import dev.rubasace.linkedin.games_tracker.user.TelegramUser;
import dev.rubasace.linkedin.games_tracker.user.TelegramUserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

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

    public TelegramGroup findGroupOrThrow(final Long chatId) throws GroupNotFoundException {
        return this.findGroup(chatId).orElseThrow(() -> new GroupNotFoundException(chatId));
    }


    @Transactional
    public TelegramGroup registerOrUpdateGroup(final Long chatId, final String title) {
        return telegramGroupRepository.findById(chatId)
                                      .map(telegramGroup -> udpateGroupData(telegramGroup, title))
                                      .orElseGet(() -> this.createGroup(chatId, title));
    }

    @Transactional
    public void addUserToGroup(final Long chatId, final Long userId, final String username) throws GroupNotFoundException {
        TelegramGroup telegramGroup = findGroupOrThrow(chatId);

        TelegramUser telegramUser = telegramUserService.findOrCreate(userId, username);
        if (telegramGroup.getMembers().contains(telegramUser)) {
            return;
        }
        telegramGroup.getMembers().add(telegramUser);
        telegramGroupRepository.save(telegramGroup);
        applicationEventPublisher.publishEvent(new UserJoinedGroupEvent(this, userId, username, chatId));
    }

    @Transactional
    public void removeUserFromGroup(final Long chatId, final Long userId) throws GroupNotFoundException {
        TelegramGroup telegramGroup = findGroupOrThrow(chatId);

        Optional<TelegramUser> telegramUser = telegramUserService.find(userId);
        if (telegramUser.isEmpty() || !telegramGroup.getMembers().contains(telegramUser.get())) {
            return;
        }
        telegramGroup.getMembers().remove(telegramUser.get());
        telegramGroupRepository.save(telegramGroup);
        applicationEventPublisher.publishEvent(new UserLeftGroupEvent(this, userId, telegramUser.get().getUserName(), chatId));
    }

    private TelegramGroup udpateGroupData(final TelegramGroup telegramGroup, final String title) {
        if (telegramGroup.getGroupName().equals(title)) {
            return telegramGroup;
        }
        telegramGroup.setGroupName(title);
        return telegramGroupRepository.save(telegramGroup);
    }


    private TelegramGroup createGroup(final Long chatId, final String title) {
        //TODO stop hardcoding the timezone and request it as part of the command interaction
        TelegramGroup telegramGroup = new TelegramGroup(chatId, title, ZoneId.of("Europe/Madrid"), EnumSet.allOf(GameType.class), Set.of());
        return telegramGroupRepository.save(telegramGroup);
    }

    public Set<GameType> listTrackedGames(final Long chatId) throws GroupNotFoundException {
        TelegramGroup telegramGroup = findGroupOrThrow(chatId);
        return telegramGroup.getTrackedGames();
    }
}
