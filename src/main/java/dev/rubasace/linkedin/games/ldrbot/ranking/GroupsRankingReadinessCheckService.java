package dev.rubasace.linkedin.games.ldrbot.ranking;

import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroup;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupAdapter;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupService;
import dev.rubasace.linkedin.games.ldrbot.session.GameSession;
import dev.rubasace.linkedin.games.ldrbot.session.GameSessionRegistrationEvent;
import dev.rubasace.linkedin.games.ldrbot.session.GameSessionService;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUser;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUserAdapter;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import dev.rubasace.linkedin.games.ldrbot.util.LinkedinTimeUtils;
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
    private final TelegramUserAdapter telegramUserAdapter;
    private final TelegramGroupAdapter telegramGroupAdapter;

    GroupsRankingReadinessCheckService(final TelegramGroupService telegramGroupService, final GameSessionService gameSessionService, final GroupRankingService groupRankingService, final TelegramUserAdapter telegramUserAdapter, final TelegramGroupAdapter telegramGroupAdapter) {
        this.telegramGroupService = telegramGroupService;
        this.gameSessionService = gameSessionService;
        this.groupRankingService = groupRankingService;
        this.telegramUserAdapter = telegramUserAdapter;
        this.telegramGroupAdapter = telegramGroupAdapter;
    }


    @Transactional
    void process(final GameSessionRegistrationEvent gameSessionRegistrationEvent) {
        Optional<TelegramGroup> telegramGroup = telegramGroupService.findGroup(gameSessionRegistrationEvent.getChatId());
        if (telegramGroup.isEmpty() || !shouldCalculateRanking(gameSessionRegistrationEvent, telegramGroup.get())) {
            return;
        }
        groupRankingService.createDailyRanking(telegramGroup.get(), gameSessionRegistrationEvent.getGameDay());

    }

    private boolean shouldCalculateRanking(final GameSessionRegistrationEvent gameSessionRegistrationEvent, final TelegramGroup telegramGroup) {
        return !gameSessionRegistrationEvent.getGameDay().equals(LinkedinTimeUtils.todayGameDay()) || allMembersDone(telegramGroup, gameSessionRegistrationEvent.getGameDay());
    }

    public boolean allMembersDone(TelegramGroup telegramGroup, final LocalDate gameDay) {
        return telegramGroup.getMembers()
                            .stream().allMatch(member -> this.submittedAllGames(member, telegramGroup, gameDay));

    }

    private boolean submittedAllGames(final TelegramUser telegramUser, final TelegramGroup telegramGroup, final LocalDate gameDay) {
        ChatInfo chatInfo = telegramGroupAdapter.adapt(telegramGroup);
        UserInfo userInfo = telegramUserAdapter.adapt(telegramUser);
        Set<GameType> submittedGames = gameSessionService.getDaySessions(chatInfo, userInfo, gameDay)
                                                         .map(GameSession::getGame)
                                                         .collect(Collectors.toSet());
        return submittedGames.containsAll(telegramGroup.getTrackedGames());
    }
}
