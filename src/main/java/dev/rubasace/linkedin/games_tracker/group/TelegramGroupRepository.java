package dev.rubasace.linkedin.games_tracker.group;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.stream.Stream;

public interface TelegramGroupRepository extends CrudRepository<TelegramGroup, Long> {

    @Query("""
                SELECT g FROM TelegramGroup g
                WHERE (
                    SELECT COUNT(score)
                    FROM DailyGameScore score
                    WHERE score.group = g AND score.date = :date
                ) < (
                    SELECT COUNT(session)
                    FROM GameSession session
                    WHERE session.group = g AND session.gameDay = :date
                )
            """)
    Stream<TelegramGroup> findGroupsWithMissingScores(LocalDate date);
}
