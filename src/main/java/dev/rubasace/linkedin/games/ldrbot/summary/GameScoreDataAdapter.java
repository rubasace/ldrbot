package dev.rubasace.linkedin.games.ldrbot.summary;

import dev.rubasace.linkedin.games.ldrbot.group.GroupInfo;
import dev.rubasace.linkedin.games.ldrbot.ranking.DailyGameScore;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
class GameScoreDataAdapter {


    List<GameScoreData> adapt(List<DailyGameScore> dailyGameScore) {
        return dailyGameScore.stream()
                             .map(this::adapt)
                             .sorted(Comparator.comparing(GameScoreData::position))
                             .toList();
    }

    private GameScoreData adapt(DailyGameScore dailyGameScore) {
        //TODO move to adapters
        GroupInfo groupInfo = new GroupInfo(dailyGameScore.getGroup().getChatId(), dailyGameScore.getGroup().getGroupName());
        UserInfo userInfo = new UserInfo(dailyGameScore.getUser().getId(), dailyGameScore.getUser().getUserName(), dailyGameScore.getUser().getFirstName(),
                                         dailyGameScore.getUser().getLastName());
        return new GameScoreData(groupInfo, userInfo, dailyGameScore.getGame(), dailyGameScore.getGameSession().getDuration(), dailyGameScore.getPosition(),
                                 dailyGameScore.getPoints());
    }
}
