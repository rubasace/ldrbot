package dev.rubasace.linkedin.games.ldrbot.web.dashboard;

import dev.rubasace.linkedin.games.ldrbot.web.leaderboard.Leaderboard;

public record GroupData(Long chatId, String title, Leaderboard leaderboard) {
}
