package dev.rubasace.linkedin.games.ldrbot.group;

import org.springframework.stereotype.Component;

@Component
public class TelegramGroupAdapter {

    public ChatInfo adapt(final TelegramGroup telegramGroup) {
        return new ChatInfo(telegramGroup.getChatId(), telegramGroup.getGroupName(), true);
    }
}
