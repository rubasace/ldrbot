package dev.rubasace.linkedin.games.ldrbot.message;

import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.User;

@Component
public class UserAdapter {

    public UserInfo adapt(final User user) {
        return new UserInfo(user.getId(), user.getUserName(), user.getFirstName(), user.getLastName());
    }
}
