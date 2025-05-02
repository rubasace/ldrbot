package dev.rubasace.linkedin.games.ldrbot.web.group;

import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Service
class GroupService {

    private final TelegramGroupService telegramGroupService;
    private final GroupDataMapper groupDataMapper;

    GroupService(final TelegramGroupService telegramGroupService, final GroupDataMapper groupDataMapper) {
        this.telegramGroupService = telegramGroupService;
        this.groupDataMapper = groupDataMapper;
    }


    GroupData getGroupData(final Long groupId) {
        return telegramGroupService.findGroup(groupId).map(groupDataMapper::map).orElseThrow();
    }
}
