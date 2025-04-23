package dev.rubasace.linkedin.games_tracker.summary;

import dev.rubasace.linkedin.games_tracker.session.GameType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record GroupDailyScore(
        Long chatId,
        LocalDate gameDay,
        Map<GameType, List<GameScoreData>> gameScores,
        List<GlobalScoreData> globalScore
) {
}