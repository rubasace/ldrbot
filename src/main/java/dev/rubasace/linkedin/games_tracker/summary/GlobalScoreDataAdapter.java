package dev.rubasace.linkedin.games_tracker.summary;

import dev.rubasace.linkedin.games_tracker.session.GameType;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
class GlobalScoreDataAdapter {

    public List<GlobalScoreData> adapt(Map<GameType, List<GameScoreData>> gameScores) {
        Map<String, GlobalScoreData> aggregated = gameScores.values().stream()
                                                            .flatMap(List::stream)
                                                            .collect(Collectors.toMap(GameScoreData::username,
                                                                                      score -> new GlobalScoreData(score.username(), score.duration(), score.points()),
                                                                                      (g1, g2) -> new GlobalScoreData(
                                                                                              g1.username(),
                                                                                              g1.totalDuration().plus(g2.totalDuration()),
                                                                                              g1.points() + g2.points()
                                                                                      )
                                                            ));

        return aggregated.values().stream()
                         .sorted(Comparator.comparing(GlobalScoreData::points).reversed()
                                           .thenComparing(GlobalScoreData::totalDuration))
                         .toList();
    }
}
