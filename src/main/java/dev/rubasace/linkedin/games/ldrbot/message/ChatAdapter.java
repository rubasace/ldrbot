package dev.rubasace.linkedin.games.ldrbot.message;

import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;

@Component
class ChatAdapter {

    ChatInfo adapt(final Chat chat) {
        return new ChatInfo(chat.getId(), chat.getTitle(), chat.isGroupChat());
    }
}
