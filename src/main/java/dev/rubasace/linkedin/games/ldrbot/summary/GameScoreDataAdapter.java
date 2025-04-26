package dev.rubasace.linkedin.games.ldrbot.summary;

import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupAdapter;
import dev.rubasace.linkedin.games.ldrbot.ranking.DailyGameScore;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUserAdapter;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
class GameScoreDataAdapter {

    private final TelegramGroupAdapter telegramGroupAdapter;
    private final TelegramUserAdapter telegramUserAdapter;

    GameScoreDataAdapter(final TelegramGroupAdapter telegramGroupAdapter, final TelegramUserAdapter telegramUserAdapter) {
        this.telegramGroupAdapter = telegramGroupAdapter;
        this.telegramUserAdapter = telegramUserAdapter;
    }


    List<GameScoreData> adapt(List<DailyGameScore> dailyGameScore) {
        return dailyGameScore.stream()
                             .map(this::adapt)
                             .sorted(Comparator.comparing(GameScoreData::position))
                             .toList();
    }

    private GameScoreData adapt(DailyGameScore dailyGameScore) {
        ChatInfo chatInfo = telegramGroupAdapter.adapt(dailyGameScore.getGroup());
        UserInfo userInfo = telegramUserAdapter.adapt(dailyGameScore.getUser());
        return new GameScoreData(chatInfo, userInfo, dailyGameScore.getGame(), dailyGameScore.getGameSession().getDuration(), dailyGameScore.getPosition(),
                                 dailyGameScore.getPoints());
    }
}
