package dev.rubasace.linkedin.games.ldrbot.web.group;

import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroup;
import org.springframework.stereotype.Component;

import java.time.format.TextStyle;
import java.util.Locale;

@Component
class GroupDataMapper {

    GroupData map(final TelegramGroup telegramGroup) {
        return new GroupData(telegramGroup.getUuid(), telegramGroup.getGroupName(),
                             telegramGroup.getTimezone().getId() + " (" + telegramGroup.getTimezone().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + ")");
    }
}
