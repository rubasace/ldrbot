package dev.rubasace.linkedin.games.ldrbot.summary;

import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
class GlobalScoreDataAdapter {

    public List<GlobalScoreData> adapt(Map<GameType, List<GameScoreData>> gameScores) {
        Map<Long, GlobalScoreData> aggregated = new HashMap<>();

        gameScores.values().stream()
                  .flatMap(List::stream)
                  .forEach(score -> aggregated.compute(score.userInfo().id(), (user, existing) -> {
                      if (existing == null) {
                          return new GlobalScoreData(score.userInfo(), score.chatInfo(), score.duration(), 0, score.points());
                      } else {
                          existing.setTotalDuration(existing.getTotalDuration().plus(score.duration()));
                          existing.setPoints(existing.getPoints() + score.points());
                          return existing;
                      }
                  }));

        List<GlobalScoreData> sorted = aggregated.values().stream()
                                                 .sorted(Comparator.comparingInt(GlobalScoreData::getPoints).reversed()
                                                                   .thenComparing(GlobalScoreData::getTotalDuration))
                                                 .toList();

        for (int i = 0; i < sorted.size(); i++) {
            if (i != 0 && tiedWithPrevious(sorted, i)) {
                sorted.get(i).setPosition(sorted.get(i - 1).getPosition());
            } else {
                sorted.get(i).setPosition(i + 1);
            }
        }

        return sorted;
    }

    private static boolean tiedWithPrevious(final List<GlobalScoreData> sorted, final int i) {
        return sorted.get(i).getPoints() == sorted.get(i - 1).getPoints() && sorted.get(i).getTotalDuration().equals(sorted.get(i - 1).getTotalDuration());
    }
}
