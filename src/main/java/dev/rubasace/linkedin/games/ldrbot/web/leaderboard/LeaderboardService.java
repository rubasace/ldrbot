package dev.rubasace.linkedin.games.ldrbot.web.leaderboard;

import dev.rubasace.linkedin.games.ldrbot.ranking.DailyScoreService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Transactional(readOnly = true)
@Service
class LeaderboardService {

    private final DailyScoreService dailyScoreService;
    private final Comparator<LeaderboardEntry> leaderboardComparator;

    LeaderboardService(final DailyScoreService dailyScoreService) {
        this.dailyScoreService = dailyScoreService;
        leaderboardComparator = Comparator
                .comparingInt(LeaderboardEntry::getTotalPoints).reversed()
                .thenComparing(LeaderboardEntry::getTotalDuration)
                .thenComparingInt(LeaderboardEntry::getTotalGames);
    }

    Leaderboard getLeaderboard(final Long groupId, final LocalDate from, final LocalDate to) {
        LocalDate fromDate = from == null ? LocalDate.ofYearDay(1991, 1) : from;
        LocalDate toDate = to == null ? LocalDate.ofYearDay(99999, 365) : to;
        Map<String, List<GameLeaderboardEntry>> leaderboardByGame = getLeaderboardByGame(groupId, fromDate, toDate);
        List<GlobalLeaderboardEntry> globalLeaderboard = calculateGlobalLeaderboard(leaderboardByGame);
        return new Leaderboard(leaderboardByGame, globalLeaderboard);
    }

    private Map<String, List<GameLeaderboardEntry>> getLeaderboardByGame(final Long groupId, final LocalDate from, final LocalDate to) {
        Map<GameLeaderboardEntry, GameLeaderboardEntry> aggregated = new HashMap<>();

        try (var scores = dailyScoreService.getGroupScores(groupId, from, to)) {
            scores.forEach(score -> {
                var user = score.getUser();
                var key = new GameLeaderboardEntry(
                        user.getId(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getUserName(),
                        StringUtils.capitalize(score.getGame().name().toLowerCase()),
                        0, Duration.ZERO, 0
                );

                var entry = aggregated.computeIfAbsent(key, k -> key);
                entry.setTotalPoints(entry.getTotalPoints() + score.getPoints());
                entry.setTotalDuration(entry.getTotalDuration().plus(score.getGameSession().getDuration()));
                entry.setTotalGames(entry.getTotalGames() + 1);
            });
        }

        return aggregated.values().stream()
                         .collect(Collectors.groupingBy(GameLeaderboardEntry::getGame, Collectors.collectingAndThen(Collectors.toList(), list -> list.stream()
                                                                                                                                                     .sorted(leaderboardComparator)
                                                                                                                                                     .toList())));

    }

    private List<GlobalLeaderboardEntry> calculateGlobalLeaderboard(final Map<String, List<GameLeaderboardEntry>> leaderboardByGame) {
        Map<Long, GlobalLeaderboardEntry> global = new HashMap<>();

        leaderboardByGame.values().stream()
                         .flatMap(List::stream)
                         .forEach(entry -> {
                             global.compute(entry.getUserId(), (id, current) -> {
                                 if (current == null) {
                                     return new GlobalLeaderboardEntry(
                                             entry.getUserId(),
                                             entry.getFirstName(),
                                             entry.getLastName(),
                                             entry.getUsername(),
                                             entry.getTotalPoints(),
                                             entry.getTotalDuration(),
                                             entry.getTotalGames()
                                     );
                                 } else {
                                     current.setTotalPoints(current.getTotalPoints() + entry.getTotalPoints());
                                     current.setTotalDuration(current.getTotalDuration().plus(entry.getTotalDuration()));
                                     current.setTotalGames(current.getTotalGames() + entry.getTotalGames());
                                     return current;
                                 }
                             });
                         });

        return global.values().stream()
                     .sorted(leaderboardComparator)
                     .toList();
    }

}
