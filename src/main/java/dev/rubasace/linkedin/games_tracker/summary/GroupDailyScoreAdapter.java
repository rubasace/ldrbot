package dev.rubasace.linkedin.games_tracker.summary;

import dev.rubasace.linkedin.games_tracker.ranking.DailyGameScore;
import dev.rubasace.linkedin.games_tracker.session.GameType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//TODO add tests to all these classes
@Component
public class GroupDailyScoreAdapter {

    private final GameScoreDataAdapter gameScoreDataAdapter;
    private final GlobalScoreDataAdapter globalScoreDataAdapter;
    private final DailyWinnerCalculator dailyWinnerCalculator;

    GroupDailyScoreAdapter(final GameScoreDataAdapter gameScoreDataAdapter,
                           final GlobalScoreDataAdapter globalScoreDataAdapter,
                           final DailyWinnerCalculator dailyWinnerCalculator) {
        this.gameScoreDataAdapter = gameScoreDataAdapter;
        this.globalScoreDataAdapter = globalScoreDataAdapter;
        this.dailyWinnerCalculator = dailyWinnerCalculator;
    }

    public GroupDailyScore adapt(final Long chatId, final Map<GameType, List<DailyGameScore>> scores) {
        Map<GameType, List<GameScoreData>> gameScores = scores.entrySet().stream()
                                                              .collect(Collectors.toMap(Map.Entry::getKey, e -> gameScoreDataAdapter.adapt(e.getValue())));

        List<GlobalScoreData> globalScoreData = globalScoreDataAdapter.adapt(gameScores);
        List<String> winners = dailyWinnerCalculator.getWinners(globalScoreData);

        return new GroupDailyScore(chatId, gameScores, globalScoreData, winners);

    }


}
