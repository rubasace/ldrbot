package dev.rubasace.linkedin.games.ldrbot.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

public interface TelegramUserRepository extends JpaRepository<TelegramUser, Long> {

    Optional<TelegramUser> findByUserNameIgnoreCase(String userName);

    /**
     * The NOT EXISTS is the day-scoped parked predicate: start inclusive, end exclusive, an open period unbounded
     * above. It is correlated to {@code g} through {@code g.parkedPeriods}, so it asks "was this user parked in
     * <em>this</em> group on {@code :gameDay}?" and not "parked anywhere". It is day-scoped rather than open-only
     * because a query whose parameter is a game day must mean what its parameter says: a player auto-un-parked today
     * holds a period ending today, and an inclusive end comparison would drop them from every reminder for the rest of
     * that day.
     */
    @Query("""
                SELECT g.chatId AS chatId, g.groupName AS groupName, g.timezone AS timeZone, u.id AS userId, u.userName AS userName, u.firstName AS firstName, u.lastName AS lastName
                FROM TelegramUser u
                LEFT JOIN u.groups g
                WHERE g.active = true
                AND NOT EXISTS (
                    SELECT 1
                    FROM g.parkedPeriods pp
                    WHERE pp.userId = u.id
                    AND pp.startGameDay <= :gameDay
                    AND (pp.endGameDay IS NULL OR pp.endGameDay > :gameDay)
                )
                AND (
                    SELECT COUNT(s)
                    FROM GameSession s
                    WHERE s.group = g AND s.user = u AND s.gameDay = :gameDay
                ) < SIZE(g.trackedGames)
            """)
    Stream<MissingSessionUserProjection> findUsersWithMissingSessions(LocalDate gameDay);

}
