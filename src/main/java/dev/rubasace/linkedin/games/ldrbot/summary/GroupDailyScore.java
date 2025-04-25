package dev.rubasace.linkedin.games.ldrbot.summary;

import dev.rubasace.linkedin.games.ldrbot.session.GameType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record GroupDailyScore(
        //TODO move to groupInfo
        Long chatId,
        LocalDate gameDay,
        Map<GameType, List<GameScoreData>> gameScores,
        List<GlobalScoreData> globalScore
) {
}