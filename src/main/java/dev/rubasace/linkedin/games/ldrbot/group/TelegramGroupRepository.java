package dev.rubasace.linkedin.games.ldrbot.group;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.stream.Stream;

public interface TelegramGroupRepository extends CrudRepository<TelegramGroup, Long> {

    @Query("""
                SELECT g FROM TelegramGroup g
                WHERE g.active = true
                AND (
                    SELECT COUNT(score)
                    FROM DailyGameScore score
                    WHERE score.group = g AND score.gameDay = :gameDay
                ) < (
                    SELECT COUNT(session)
                    FROM GameSession session
                    WHERE session.group = g AND session.gameDay = :gameDay
                )
            """)
    Stream<TelegramGroup> findGroupsWithMissingScores(LocalDate gameDay);
}
