package dev.rubasace.linkedin.games.ldrbot.web.dashboard;

import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroup;
import dev.rubasace.linkedin.games.ldrbot.web.leaderboard.Leaderboard;
import org.springframework.stereotype.Component;

@Component
class GroupDataMapper {


    GroupData map(final TelegramGroup telegramGroup, final Leaderboard leaderboard) {
        return new GroupData(telegramGroup.getChatId(), telegramGroup.getGroupName(), leaderboard);
    }
}
