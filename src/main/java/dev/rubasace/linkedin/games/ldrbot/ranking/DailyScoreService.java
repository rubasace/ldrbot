package dev.rubasace.linkedin.games.ldrbot.ranking;

import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.util.LinkedinTimeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

@Transactional(readOnly = true)
@Service
public class DailyScoreService {

    private final DailyScoreRepository dailyScoreRepository;

    DailyScoreService(final DailyScoreRepository dailyScoreRepository) {
        this.dailyScoreRepository = dailyScoreRepository;
    }

    public Stream<DailyGameScore> getGroupScores(String uuid, LocalDate startDate, LocalDate endDate) {
        return dailyScoreRepository.findAllByGroupUuidAndGameDayBetween(uuid, startDate, endDate);
    }


    @Transactional
    public List<DailyGameScore> updateDailyScores(final List<DailyGameScore> scores, final Long chatId, final GameType game) {
        cleanupOldReferences(scores);
        dailyScoreRepository.deleteAllByGroupChatIdAndGameDayAndGame(chatId, LinkedinTimeUtils.todayGameDay(), game);
        return dailyScoreRepository.saveAll(scores);
    }

    private static void cleanupOldReferences(final List<DailyGameScore> scores) {
        scores.forEach(score -> score.getGameSession().setDailyGameScore(null));
    }
}
