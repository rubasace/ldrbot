package dev.rubasace.linkedin.games.ldrbot.user;

import org.springframework.stereotype.Component;

@Component
public class TelegramUserAdapter {

    public UserInfo adapt(final TelegramUser telegramUser) {
        return new UserInfo(telegramUser.getId(), telegramUser.getUserName(), telegramUser.getFirstName(), telegramUser.getLastName());
    }
}
