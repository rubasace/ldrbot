package dev.rubasace.linkedin.games.ldrbot.reminder;

import dev.rubasace.linkedin.games.ldrbot.user.MissingSessionUserProjection;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import org.springframework.stereotype.Component;

@Component
class MissingSessionUserProjectionUserInfoAdapter {

    UserInfo adapt(final MissingSessionUserProjection missingSessionUserProjection) {
        return new UserInfo(missingSessionUserProjection.getUserId(), missingSessionUserProjection.getUserName(), missingSessionUserProjection.getFirstName(),
                            missingSessionUserProjection.getLastName());
    }
}

