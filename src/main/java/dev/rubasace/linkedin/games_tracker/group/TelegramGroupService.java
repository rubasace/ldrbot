package dev.rubasace.linkedin.games_tracker.group;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public TelegramGroup registerOrUpdateGroup(final Long chatId, final String title) {
        return telegramGroupRepository.findById(chatId)
                                      .map(telegramGroup -> udpateGroupData(telegramGroup, title))
                                      .orElseGet(() -> this.createGroup(chatId, title));
    }

    @Transactional
    public <S extends TelegramGroup> S save(final S entity) {
        return telegramGroupRepository.save(entity);
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
