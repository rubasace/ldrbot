package dev.rubasace.linkedin.games.ldrbot.reminder;

import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.user.MissingSessionUserProjection;
import org.springframework.stereotype.Component;

@Component
class MissingSessionUserProjectionChatInfoAdapter {

    ChatInfo adapt(final MissingSessionUserProjection missingSessionUserProjection) {
        return new ChatInfo(missingSessionUserProjection.getChatId(), missingSessionUserProjection.getGroupName(), true);
    }
}

