package dev.rubasace.linkedin.games_tracker.group;

import dev.rubasace.linkedin.games_tracker.user.TelegramUser;
import dev.rubasace.linkedin.games_tracker.user.TelegramUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;

@Transactional(readOnly = true)
@Service
public class TelegramGroupService {

    private final TelegramGroupRepository telegramGroupRepository;
    private final TelegramUserService telegramUserService;

    TelegramGroupService(final TelegramGroupRepository telegramGroupRepository, final TelegramUserService telegramUserService) {
        this.telegramGroupRepository = telegramGroupRepository;
        this.telegramUserService = telegramUserService;
    }

    public Optional<TelegramGroup> findGroup(final Long chatId) {
        return telegramGroupRepository.findById(chatId);
    }

    @Transactional
    public TelegramGroup registerOrUpdateGroup(final Long chatId, final String title) {
        return telegramGroupRepository.findById(chatId)
                                      .map(telegramGroup -> udpateGroupData(telegramGroup, title))
                                      .orElseGet(() -> this.createGroup(chatId, title));
    }

    @Transactional
    public boolean addUserToGroup(final Long chatId, final Long userId, final String username) throws GroupNotFoundException {
        TelegramGroup telegramGroup = this.findGroup(chatId).orElseThrow(GroupNotFoundException::new);

        TelegramUser telegramUser = telegramUserService.findOrCreate(userId, username);
        if (telegramGroup.getMembers().contains(telegramUser)) {
            return false;
        }
        telegramGroup.getMembers().add(telegramUser);
        telegramGroupRepository.save(telegramGroup);
        return true;
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
        TelegramGroup telegramGroup = new TelegramGroup(chatId, title, ZoneId.of("Europe/Madrid"), Set.of());
        return telegramGroupRepository.save(telegramGroup);
    }
}
