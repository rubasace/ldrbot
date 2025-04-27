package dev.rubasace.linkedin.games.ldrbot.ranking;

import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.group.GroupNotFoundException;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroup;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupService;
import dev.rubasace.linkedin.games.ldrbot.session.GameSession;
import dev.rubasace.linkedin.games.ldrbot.session.GameSessionService;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.summary.GroupDailyScore;
import dev.rubasace.linkedin.games.ldrbot.summary.GroupDailyScoreAdapter;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUser;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Transactional(readOnly = true)
@Service
public class GroupRankingService {

    private final TelegramGroupService telegramGroupService;
    private final GameSessionService gameSessionService;
    private final DailyGameScoreCalculator dailyGameScoreCalculator;
    private final DailyScoreService dailyScoreService;
    private final GroupDailyScoreAdapter groupDailyScoreAdapter;
    private final ApplicationEventPublisher applicationEventPublisher;

    GroupRankingService(final TelegramGroupService telegramGroupService, final GameSessionService gameSessionService,
                        final DailyGameScoreCalculator dailyGameScoreCalculator,
                        final DailyScoreService dailyScoreService,
                        final GroupDailyScoreAdapter groupDailyScoreAdapter,
                        final ApplicationEventPublisher applicationEventPublisher) {
        this.telegramGroupService = telegramGroupService;
        this.gameSessionService = gameSessionService;
        this.dailyGameScoreCalculator = dailyGameScoreCalculator;
        this.dailyScoreService = dailyScoreService;
        this.groupDailyScoreAdapter = groupDailyScoreAdapter;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public void createDailyRanking(final ChatInfo chatInfo, final LocalDate gameDay) throws GroupNotFoundException {
        TelegramGroup telegramGroup = telegramGroupService.findGroupOrThrow(chatInfo);
        createDailyRanking(telegramGroup, gameDay);
    }

    @Transactional
    public void createDailyRanking(final TelegramGroup telegramGroup, final LocalDate gameDay) {
        Set<Long> userIds = telegramGroup.getMembers().stream()
                                         .map(TelegramUser::getId)
                                         .collect(Collectors.toSet());

        Map<GameType, List<GameSession>> groupSessions = gameSessionService.getDaySessions(userIds, telegramGroup.getChatId(), gameDay)
                                                                           .collect(Collectors.groupingBy(GameSession::getGame));


        Map<GameType, List<DailyGameScore>> gameScores = new HashMap<>();
        for (Map.Entry<GameType, List<GameSession>> entry : groupSessions.entrySet()) {
            GameType gameType = entry.getKey();
            if (!telegramGroup.getTrackedGames().contains(gameType)) {
                continue;
            }
            List<GameSession> sessions = entry.getValue();
            List<DailyGameScore> dailyGameScores = dailyGameScoreCalculator.calculateScores(sessions, telegramGroup);
            gameScores.put(gameType, dailyScoreService.updateDailyScores(dailyGameScores, telegramGroup.getChatId(), gameType));
        }
        notifyRankingCreation(telegramGroup, gameScores, gameDay);
    }

    private void notifyRankingCreation(final TelegramGroup telegramGroup, final Map<GameType, List<DailyGameScore>> gameScores, final LocalDate gameDay) {
        GroupDailyScore groupDailyScore = groupDailyScoreAdapter.adapt(telegramGroup, gameScores, gameDay);
        applicationEventPublisher.publishEvent(new GroupDailyScoreCreatedEvent(this, groupDailyScore));
    }

}
