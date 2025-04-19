package dev.rubasace.linkedin.games_tracker.session;

import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public interface GameSessionRepository extends CrudRepository<GameSession, UUID> {

    boolean existsByUserIdAndGameAndGameDay(Long UserId, GameType game, LocalDate gameDay);

    @Transactional
    void deleteByUserIdAndGameAndGameDay(Long UserId, GameType game, LocalDate gameDay);

    Stream<GameSession> getByUserIdAndGameDay(Long UserId, LocalDate gameDay);

    Stream<GameSession> getByUserIdInAndGameDay(Set<Long> UserIds, LocalDate gameDay);

    @Transactional
    void deleteByUserIdAndGameDay(Long UserId, LocalDate gameDay);
}
