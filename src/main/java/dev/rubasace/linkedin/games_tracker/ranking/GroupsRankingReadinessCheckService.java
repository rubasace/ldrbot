package dev.rubasace.linkedin.games_tracker.ranking;

import dev.rubasace.linkedin.games_tracker.group.TelegramGroup;
import dev.rubasace.linkedin.games_tracker.session.GameSession;
import dev.rubasace.linkedin.games_tracker.session.GameSessionRegistrationEvent;
import dev.rubasace.linkedin.games_tracker.session.GameSessionService;
import dev.rubasace.linkedin.games_tracker.session.GameType;
import dev.rubasace.linkedin.games_tracker.user.TelegramUser;
import dev.rubasace.linkedin.games_tracker.user.TelegramUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
class GroupsRankingReadinessCheckService {

    private final TelegramUserService telegramUserService;
    private final GameSessionService gameSessionService;
    private final GroupRankingService groupRankingService;

    GroupsRankingReadinessCheckService(final TelegramUserService telegramUserService, final GameSessionService gameSessionService, final GroupRankingService groupRankingService) {
        this.telegramUserService = telegramUserService;
        this.gameSessionService = gameSessionService;
        this.groupRankingService = groupRankingService;
    }

    @Transactional
    void process(final GameSessionRegistrationEvent gameSessionRegistrationEvent) {
        TelegramUser telegramUser = telegramUserService.find(gameSessionRegistrationEvent.getUserId());
        telegramUser.getGroups().stream()
                    .filter(this::allMembersDone)
                    .forEach(groupRankingService::createDailyRanking);
    }

    @Transactional
    public boolean allMembersDone(TelegramGroup telegramGroup) {
        return telegramGroup.getMembers()
                            .stream().allMatch(member -> this.submittedAllGames(member, telegramGroup.getTrackedGames()));

    }

    private boolean submittedAllGames(final TelegramUser telegramUser, final Set<GameType> trackedGames) {
        Set<GameType> submittedGames = gameSessionService.getTodaySessions(telegramUser.getId())
                                                         .map(GameSession::getGame)
                                                         .collect(Collectors.toSet());
        return submittedGames.containsAll(trackedGames);
    }
}
