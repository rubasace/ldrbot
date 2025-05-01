package dev.rubasace.linkedin.games.ldrbot.web.dashboard;

import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroup;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupService;
import dev.rubasace.linkedin.games.ldrbot.web.leaderboard.Leaderboard;
import dev.rubasace.linkedin.games.ldrbot.web.leaderboard.LeaderboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Transactional(readOnly = true)
@Service
class DashboardService {

    private final TelegramGroupService telegramGroupService;
    private final LeaderboardService leaderboardService;
    private final GroupDataMapper groupDataMapper;

    DashboardService(final TelegramGroupService telegramGroupService, final LeaderboardService leaderboardService, final GroupDataMapper groupDataMapper) {
        this.telegramGroupService = telegramGroupService;
        this.leaderboardService = leaderboardService;
        this.groupDataMapper = groupDataMapper;
    }


    GroupData getGroupData(final Long groupId) {

        Leaderboard leaderboard = leaderboardService.getLeaderboard(groupId, LocalDate.ofYearDay(1991, 1), LocalDate.ofYearDay(99999, 365));

        return telegramGroupService.findGroup(groupId).map((TelegramGroup telegramGroup) -> groupDataMapper.map(telegramGroup, leaderboard)).orElseThrow();
    }
}
