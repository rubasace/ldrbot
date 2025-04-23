package dev.rubasace.linkedin.games_tracker.ranking;

import dev.rubasace.linkedin.games_tracker.session.GameType;
import dev.rubasace.linkedin.games_tracker.util.LinkedinTimeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(readOnly = true)
@Service
public class DailyScoreService {

    private final DailyScoreRepository dailyScoreRepository;

    DailyScoreService(final DailyScoreRepository dailyScoreRepository) {
        this.dailyScoreRepository = dailyScoreRepository;
    }

    @Transactional
    public List<DailyGameScore> updateDailyScores(final List<DailyGameScore> scores, final Long chatId, final GameType game) {
        //TODO investigate if this is still breaking and fix properly.
        cleanupOldReferences(scores);
        dailyScoreRepository.deleteAllByGroupChatIdAndGameDayAndGame(chatId, LinkedinTimeUtils.todayGameDay(), game);
        return dailyScoreRepository.saveAll(scores);
    }

    private static void cleanupOldReferences(final List<DailyGameScore> scores) {
        scores.forEach(score -> score.getGameSession().setDailyGameScore(null));
    }
}
