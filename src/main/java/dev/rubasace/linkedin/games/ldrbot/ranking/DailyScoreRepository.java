package dev.rubasace.linkedin.games.ldrbot.ranking;

import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

public interface DailyScoreRepository extends JpaRepository<DailyGameScore, UUID> {

    @Transactional
    void deleteAllByGroupChatIdAndGameDayAndGame(Long chatId, LocalDate gameDay, GameType game);
}
