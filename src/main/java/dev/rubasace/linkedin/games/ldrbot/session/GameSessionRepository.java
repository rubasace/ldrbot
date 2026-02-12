package dev.rubasace.linkedin.games.ldrbot.session;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
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

    long countByGroupChatIdAndGame(Long chatId, GameType game);

    @Query("SELECT DISTINCT gs.duration FROM GameSession gs WHERE gs.group.chatId = :chatId AND gs.game = :game ORDER BY gs.duration ASC")
    List<Duration> findDistinctDurationsOrderedAsc(@Param("chatId") Long chatId, @Param("game") GameType game);
}
