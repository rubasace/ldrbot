package dev.rubasace.linkedin.games_tracker.summary;

import dev.rubasace.linkedin.games_tracker.session.GameType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record GroupDailyScore(
        Long chatId,
        Map<GameType, List<GameScoreData>> gameScores,
        LocalDate date,
        List<GlobalScoreData> globalScore,
        List<String> winners
) {
}