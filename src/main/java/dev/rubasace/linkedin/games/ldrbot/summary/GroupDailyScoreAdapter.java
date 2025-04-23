package dev.rubasace.linkedin.games.ldrbot.summary;

import dev.rubasace.linkedin.games.ldrbot.ranking.DailyGameScore;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class GroupDailyScoreAdapter {

    private final GameScoreDataAdapter gameScoreDataAdapter;
    private final GlobalScoreDataAdapter globalScoreDataAdapter;

    GroupDailyScoreAdapter(final GameScoreDataAdapter gameScoreDataAdapter,
                           final GlobalScoreDataAdapter globalScoreDataAdapter) {
        this.gameScoreDataAdapter = gameScoreDataAdapter;
        this.globalScoreDataAdapter = globalScoreDataAdapter;
    }

    public GroupDailyScore adapt(final Long chatId, final Map<GameType, List<DailyGameScore>> scores, final LocalDate gameDay) {
        Map<GameType, List<GameScoreData>> gameScores = scores.entrySet().stream()
                                                              .collect(Collectors.toMap(Map.Entry::getKey, e -> gameScoreDataAdapter.adapt(e.getValue())));

        List<GlobalScoreData> globalScoreData = globalScoreDataAdapter.adapt(gameScores);

        return new GroupDailyScore(chatId, gameDay, gameScores, globalScoreData);

    }

}
