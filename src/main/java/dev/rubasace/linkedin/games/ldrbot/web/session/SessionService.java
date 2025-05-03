package dev.rubasace.linkedin.games.ldrbot.web.session;

import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroup;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupService;
import dev.rubasace.linkedin.games.ldrbot.session.GameSessionService;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Transactional(readOnly = true)
@Service
class SessionService {

    private final TelegramGroupService telegramGroupService;
    private final GameSessionService gameSessionService;
    private final GameSessionAdapter gameSessionAdapter;

    SessionService(final TelegramGroupService telegramGroupService, final GameSessionService gameSessionService, final GameSessionAdapter gameSessionAdapter) {
        this.telegramGroupService = telegramGroupService;
        this.gameSessionService = gameSessionService;
        this.gameSessionAdapter = gameSessionAdapter;
    }

    List<Session> getSessions(final String groupId) {
        TelegramGroup telegramGroup = telegramGroupService.findGroup(groupId).orElseThrow();
        Set<Long> userIds = telegramGroup.getMembers().stream()
                                         .map(TelegramUser::getId)
                                         .collect(Collectors.toSet());
        return gameSessionService.getGameSessions(groupId, userIds)
                                 .map(gameSessionAdapter::adapt)
                                 .toList();
    }
}
