package dev.rubasace.linkedin.games.ldrbot.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

public interface TelegramUserRepository extends JpaRepository<TelegramUser, Long> {

    Optional<TelegramUser> findByUserName(String userName);

    @Query("""
                SELECT g.chatId AS chatId, g.groupName AS groupName, g.timezone AS timeZone, u.id AS userId, u.userName AS userName, u.firstName AS firstName, u.lastName AS lastName
                FROM TelegramUser u
                LEFT JOIN u.groups g
                WHERE g.active = true
                AND (
                    SELECT COUNT(s)
                    FROM GameSession s
                    WHERE s.group = g AND s.user = u AND s.gameDay = :gameDay
                ) < SIZE(g.trackedGames)
            """)
    Stream<MissingSessionUserProjection> findUsersWithMissingSessions(LocalDate gameDay);

}
