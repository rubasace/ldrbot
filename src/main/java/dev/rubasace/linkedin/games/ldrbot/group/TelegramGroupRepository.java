package dev.rubasace.linkedin.games.ldrbot.group;

import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public interface TelegramGroupRepository extends CrudRepository<TelegramGroup, Long> {

    /**
     * The asymmetry is deliberate and both halves are binding: the session count is filtered to the players who were
     * taking part in this group on {@code :gameDay}, and the score count is <em>not</em>.
     * <p>
     * Filtering only the right-hand side can only shrink it, so the days this job selects after the feature are a
     * subset of the days it selects today. Filtering the score count as well would shrink the left-hand side too, which
     * makes {@code <} easier to satisfy and adds selections of <em>past</em> game days that today's code never makes.
     * The stale score rows of parked players are what inflate the unfiltered left-hand side, and that inflation is the
     * signal that the day already holds a full set of rows and must not be touched.
     * <p>
     * The NOT EXISTS is correlated to {@code g} through {@code g.parkedPeriods}: uncorrelated it would mean "parked in
     * any group", which parses, boots, and silently stops a never-parked group from ranking a day whose only unscored
     * sessions belong to a member parked somewhere else.
     */
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
                    AND NOT EXISTS (
                        SELECT 1
                        FROM g.parkedPeriods pp
                        WHERE pp.userId = session.user.id
                        AND pp.startGameDay <= :gameDay
                        AND (pp.endGameDay IS NULL OR pp.endGameDay > :gameDay)
                    )
                )
            """)
    Stream<TelegramGroup> findGroupsWithMissingScores(LocalDate gameDay);

    Optional<TelegramGroup> findByUuid(String uuid);
}
