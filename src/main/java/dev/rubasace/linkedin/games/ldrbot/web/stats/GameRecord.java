package dev.rubasace.linkedin.games.ldrbot.web.stats;

import java.time.LocalDate;

public record GameRecord(String game, int seconds, Long userId, String username, String firstName, LocalDate date) {
}
