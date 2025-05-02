package dev.rubasace.linkedin.games.ldrbot.web.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class GroupStats {

    private Map<String, GameAverage> averagePerGame;
    private Map<String, GameRecord> recordByGame;
}
