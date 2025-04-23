package dev.rubasace.linkedin.games.ldrbot.ranking;

import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroup;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupService;
import dev.rubasace.linkedin.games.ldrbot.session.GameSession;
import dev.rubasace.linkedin.games.ldrbot.session.GameSessionRegistrationEvent;
import dev.rubasace.linkedin.games.ldrbot.session.GameSessionService;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
        if (telegramGroup.isEmpty() || !allMembersDone(telegramGroup.get(), gameSessionRegistrationEvent.getGameDay())) {
            return;
        }
        groupRankingService.createDailyRanking(telegramGroup.get(), gameSessionRegistrationEvent.getGameDay());
    }

    public boolean allMembersDone(TelegramGroup telegramGroup, final LocalDate gameDay) {
        return telegramGroup.getMembers()
                            .stream().allMatch(member -> this.submittedAllGames(member, telegramGroup, gameDay));

    }

    private boolean submittedAllGames(final TelegramUser telegramUser, final TelegramGroup telegramGroup, final LocalDate gameDay) {
        Set<GameType> submittedGames = gameSessionService.getDaySessions(telegramUser.getId(), telegramGroup.getChatId(), gameDay)
                                                         .map(GameSession::getGame)
                                                         .collect(Collectors.toSet());
        return submittedGames.containsAll(telegramGroup.getTrackedGames());
    }
}
