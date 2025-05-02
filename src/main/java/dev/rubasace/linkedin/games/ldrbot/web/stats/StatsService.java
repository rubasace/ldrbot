package dev.rubasace.linkedin.games.ldrbot.web.stats;

import dev.rubasace.linkedin.games.ldrbot.group.GroupNotFoundException;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupService;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Transactional(readOnly = true)
@Service
class StatsService {

    private final StatsRepository statsRepository;
    private final TelegramGroupService telegramGroupService;
    private final GameRecordProjectionAdapter gameSessionProjectionAdapter;

    StatsService(final StatsRepository statsRepository, final TelegramGroupService telegramGroupService, final GameRecordProjectionAdapter gameSessionProjectionAdapter) {
        this.statsRepository = statsRepository;
        this.telegramGroupService = telegramGroupService;
        this.gameSessionProjectionAdapter = gameSessionProjectionAdapter;
    }

    //TODO standardize REST error handling
    GroupStats getStats(Long groupId) throws GroupNotFoundException {
        Set<GameType> trackedGames = telegramGroupService.listTrackedGames(groupId);
        Map<String, GameAverage> averagesByGame = calculateAveragesByGame(groupId, trackedGames);
        Map<String, GameRecord> recordsByGame = calculateRecordsByGame(groupId, trackedGames);
        return new GroupStats(averagesByGame, recordsByGame);
    }

    private Map<String, GameAverage> calculateAveragesByGame(final Long groupId, final Set<GameType> trackedGames) {
        Map<String, Long> totalDurations = new HashMap<>();
        Map<String, Integer> countPerGame = new HashMap<>();

        try (Stream<GameSessionProjection> stream = statsRepository.findSessionsPerGame(groupId, trackedGames)) {
            stream.forEach(p -> {
                String game = p.getGame().name();
                long seconds = p.getDuration().toSeconds();

                totalDurations.merge(game, seconds, Long::sum);
                countPerGame.merge(game, 1, Integer::sum);
            });
        }

        Map<String, Double> averages = new HashMap<>();
        for (var entry : totalDurations.entrySet()) {
            String game = entry.getKey();
            long total = entry.getValue();
            int count = countPerGame.get(game);
            averages.put(game, (double) total / count);
        }

        return averages.entrySet()
                       .stream()
                       .collect(Collectors.toMap(Map.Entry::getKey,
                                                 e -> new GameAverage(StringUtils.capitalize(e.getKey().toLowerCase()), e.getValue(), countPerGame.get(e.getKey()))));
    }

    private Map<String, GameRecord> calculateRecordsByGame(final Long groupId, final Set<GameType> trackedGames) {
        Map<String, GameRecord> recordsByGame = new HashMap<>();
        try (Stream<GameSessionProjection> stream = statsRepository.findSessionsPerGame(groupId, trackedGames)) {
            stream.forEach(gameSessionProjection -> {
                String game = gameSessionProjection.getGame().name();
                int seconds = (int) gameSessionProjection.getDuration().toSeconds();
                GameRecord existing = recordsByGame.get(game);
                if (existing == null || seconds < existing.seconds()) {
                    recordsByGame.put(game, gameSessionProjectionAdapter.adapt(gameSessionProjection));
                }
            });
        }

        return recordsByGame;
    }
}
