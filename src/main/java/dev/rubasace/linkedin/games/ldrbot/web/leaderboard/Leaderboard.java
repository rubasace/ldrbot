package dev.rubasace.linkedin.games.ldrbot.web.leaderboard;

import java.util.List;
import java.util.Map;


public record Leaderboard(Map<String, List<GameLeaderboardEntry>> gamesLeaderboard, List<GlobalLeaderboardEntry> globalLeaderboard) {
}

