package dev.rubasace.linkedin.games.ldrbot.session;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public interface GameSessionRepository extends JpaRepository<GameSession, UUID> {

    boolean existsByUserIdAndGroupChatIdAndGameAndGameDay(Long UserId, Long chatId, GameType game, LocalDate gameDay);

    Stream<GameSession> getByGroupUuidAndUserIdInOrderByGameDayDescRegisteredAtDesc(String uuid, Set<Long> userIds);

    Optional<GameSession> getByUserIdAndGroupChatIdAndGameAndGameDay(Long UserId, Long chatId, GameType game, LocalDate gameDay);
    @Transactional
    void deleteByUserIdAndGroupChatIdAndGameAndGameDay(Long UserId, Long chatId, GameType game, LocalDate gameDay);

    Stream<GameSession> getByUserIdAndGroupChatIdAndGameDay(Long UserId, Long chatId, LocalDate gameDay);

    Stream<GameSession> getByUserIdInAndGroupChatIdAndGameDay(Set<Long> UserIds, Long chatId, LocalDate gameDay);

    @Transactional
    void deleteByUserIdAndGroupChatIdAndGameDay(Long UserId, Long chatId, LocalDate gameDay);

    // REQUIRES_NEW is deliberate: it is the AC15 mechanism (issue #7) — record detection gets its own
    // transaction so a failure here cannot roll back the caller's registering transaction. Any second
    // caller must re-decide it: this suspends the caller's transaction and takes a second connection.
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    Optional<GameSession> getTop1ByGroupChatIdAndGameAndIdNotOrderByDurationAsc(Long chatId, GameType game, UUID id);
}
