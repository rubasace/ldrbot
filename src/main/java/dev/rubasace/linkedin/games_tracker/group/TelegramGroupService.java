package dev.rubasace.linkedin.games_tracker.group;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;

import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;

@Transactional(readOnly = true)
@Service
public class TelegramGroupService {

    private final TelegramGroupRepository telegramGroupRepository;

    public TelegramGroupService(final TelegramGroupRepository telegramGroupRepository) {
        this.telegramGroupRepository = telegramGroupRepository;
    }

    public Optional<TelegramGroup> findGroup(final Long chatId) {
        return telegramGroupRepository.findById(chatId);
    }

    @Transactional
    public TelegramGroup registerOrUpdateGroup(final Chat chat) {
        if (!chat.isGroupChat()) {
            throw new IllegalArgumentException("Chat must be a group");
        }
        return telegramGroupRepository.findById(chat.getId())
                                      .map(telegramGroup -> udpateGroupData(telegramGroup, chat))
                                      .orElseGet(() -> this.createGroup(chat));
    }

    @Transactional
    public <S extends TelegramGroup> S save(final S entity) {
        return telegramGroupRepository.save(entity);
    }

    private TelegramGroup udpateGroupData(final TelegramGroup telegramGroup, final Chat chat) {
        if (telegramGroup.getGroupName().equals(chat.getTitle())) {
            return telegramGroup;
        }
        telegramGroup.setGroupName(chat.getTitle());
        return telegramGroupRepository.save(telegramGroup);
    }


    private TelegramGroup createGroup(final Chat chat) {
        //TODO stop hardcoding the timezone and request it as part of the command interaction
        TelegramGroup telegramGroup = new TelegramGroup(chat.getId(), chat.getTitle(), ZoneId.of("Europe/Madrid"), Set.of());
        return telegramGroupRepository.save(telegramGroup);
    }
}
