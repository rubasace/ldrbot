package dev.rubasace.linkedin.games_tracker.ranking;

import dev.rubasace.linkedin.games_tracker.session.GameType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.UUID;

public interface DailyScoreRepository extends JpaRepository<DailyGameScore, UUID> {

    void deleteAllByGroupChatIdAndDateAndGame(Long chatId, LocalDate date, GameType game);
}
