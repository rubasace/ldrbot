package dev.rubasace.linkedin.games.ldrbot.summary;

import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.session.GameInfo;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record GroupDailyScore(
        ChatInfo chatInfo,
        LocalDate gameDay,
        Map<GameInfo, List<GameScoreData>> gameScores,
        List<GlobalScoreData> globalScore
) {
}