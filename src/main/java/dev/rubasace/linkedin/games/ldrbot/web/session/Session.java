package dev.rubasace.linkedin.games.ldrbot.web.session;

import java.time.LocalDate;
import java.time.ZonedDateTime;

public record Session(Long userId, String username, String firstName, String game, int seconds, LocalDate date, ZonedDateTime registeredAt) {
}
