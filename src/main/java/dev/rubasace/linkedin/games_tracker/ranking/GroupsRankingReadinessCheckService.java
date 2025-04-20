package dev.rubasace.linkedin.games_tracker.ranking;

import dev.rubasace.linkedin.games_tracker.group.TelegramGroup;
import dev.rubasace.linkedin.games_tracker.group.TelegramGroupService;
import dev.rubasace.linkedin.games_tracker.session.GameSession;
import dev.rubasace.linkedin.games_tracker.session.GameSessionRegistrationEvent;
import dev.rubasace.linkedin.games_tracker.session.GameSessionService;
import dev.rubasace.linkedin.games_tracker.session.GameType;
import dev.rubasace.linkedin.games_tracker.user.TelegramUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Transactional(readOnly = true)
@Service
class GroupsRankingReadinessCheckService {

    private final TelegramGroupService telegramGroupService;
    private final GameSessionService gameSessionService;
    private final GroupRankingService groupRankingService;

    GroupsRankingReadinessCheckService(final TelegramGroupService telegramGroupService, final GameSessionService gameSessionService, final GroupRankingService groupRankingService) {
        this.telegramGroupService = telegramGroupService;
        this.gameSessionService = gameSessionService;
        this.groupRankingService = groupRankingService;
    }


    @Transactional
    void process(final GameSessionRegistrationEvent gameSessionRegistrationEvent) {
        Optional<TelegramGroup> telegramGroup = telegramGroupService.findGroup(gameSessionRegistrationEvent.getChatId());
        if (telegramGroup.isEmpty() || !allMembersDone(telegramGroup.get())) {
            return;
        }
        groupRankingService.createDailyRanking(telegramGroup.get());
    }

    public boolean allMembersDone(TelegramGroup telegramGroup) {
        return telegramGroup.getMembers()
                            .stream().allMatch(member -> this.submittedAllGames(member, telegramGroup));

    }

    private boolean submittedAllGames(final TelegramUser telegramUser, final TelegramGroup telegramGroup) {
        Set<GameType> submittedGames = gameSessionService.getTodaySessions(telegramUser.getId(), telegramGroup.getChatId())
                                                         .map(GameSession::getGame)
                                                         .collect(Collectors.toSet());
        return submittedGames.containsAll(telegramGroup.getTrackedGames());
    }
}
