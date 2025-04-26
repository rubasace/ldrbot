package dev.rubasace.linkedin.games.ldrbot.user;

import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
class TelegramUserMapper {

    TelegramUser map(final UserInfo userInfo) {
        return new TelegramUser(userInfo.id(), userInfo.userName(), userInfo.firstName(), userInfo.lastName(), new HashSet<>(), new HashSet<>());
    }
}
