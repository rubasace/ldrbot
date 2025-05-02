package dev.rubasace.linkedin.games.ldrbot.web.stats;

import dev.rubasace.linkedin.games.ldrbot.session.GameSession;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Set;
import java.util.stream.Stream;

public interface StatsRepository extends Repository<GameSession, Long> {

    @Query("""
                SELECT s.game AS game,
                       gs.duration AS duration,
                       u.id AS userId,
                       u.userName AS username,
                       u.firstName AS firstName,
                       s.gameDay AS date
                FROM DailyGameScore s
                JOIN s.user u
                JOIN s.gameSession gs
                WHERE s.group.uuid = :groupId
                  AND s.game IN :trackedGames
                ORDER BY s.game, s.gameDay ASC
            """)
    Stream<GameSessionProjection> findSessionsPerGame(@Param("groupId") String groupId, @Param("trackedGames") Set<GameType> trackedGames);
}
