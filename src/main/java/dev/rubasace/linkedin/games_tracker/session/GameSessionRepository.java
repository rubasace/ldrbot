package dev.rubasace.linkedin.games_tracker.session;

import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.UUID;

public interface GameSessionRepository extends CrudRepository<GameSession, UUID> {

    boolean existsByTelegramUserIdAndGameAndGameDay(Long telegramUserId, GameType game, LocalDate gameDay);

    void deleteByTelegramUserIdAndGameAndGameDay(Long telegramUserId, GameType game, LocalDate gameDay);
}
