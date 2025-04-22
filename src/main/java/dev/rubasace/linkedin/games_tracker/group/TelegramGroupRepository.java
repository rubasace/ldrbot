package dev.rubasace.linkedin.games_tracker.group;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.stream.Stream;
public interface TelegramGroupRepository extends CrudRepository<TelegramGroup, Long> {

    @Query("""
                SELECT g FROM TelegramGroup g
                WHERE (
                    SELECT COUNT(s)
                    FROM DailyGameScore s
                    WHERE s.group = g AND s.date = :date
                ) < (
                    SIZE(g.members) * SIZE(g.trackedGames)
                )
            """)
    Stream<TelegramGroup> findGroupsWithMissingScores(LocalDate date);
}
