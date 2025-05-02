package dev.rubasace.linkedin.games.ldrbot.web.group;

import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroup;
import org.springframework.stereotype.Component;

@Component
class GroupDataMapper {

    GroupData map(final TelegramGroup telegramGroup) {
        return new GroupData(telegramGroup.getUuid(), telegramGroup.getGroupName());
    }
}
