package dev.rubasace.linkedin.games.ldrbot.message;

import dev.rubasace.linkedin.games.ldrbot.group.GroupInfo;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;

@Component
class GroupInfoAdapter {

    GroupInfo adapt(final Chat chat) {
        return new GroupInfo(chat.getId(), chat.getTitle());
    }
}
