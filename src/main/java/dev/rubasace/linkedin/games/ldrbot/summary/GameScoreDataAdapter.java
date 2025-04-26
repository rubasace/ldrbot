package dev.rubasace.linkedin.games.ldrbot.summary;

import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupAdapter;
import dev.rubasace.linkedin.games.ldrbot.ranking.DailyGameScore;
import dev.rubasace.linkedin.games.ldrbot.session.GameInfo;
import dev.rubasace.linkedin.games.ldrbot.session.GameTypeAdapter;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUserAdapter;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
class GameScoreDataAdapter {

    private final TelegramGroupAdapter telegramGroupAdapter;
    private final TelegramUserAdapter telegramUserAdapter;
    private final GameTypeAdapter gameTypeAdapter;

    GameScoreDataAdapter(final TelegramGroupAdapter telegramGroupAdapter, final TelegramUserAdapter telegramUserAdapter, final GameTypeAdapter gameTypeAdapter) {
        this.telegramGroupAdapter = telegramGroupAdapter;
        this.telegramUserAdapter = telegramUserAdapter;
        this.gameTypeAdapter = gameTypeAdapter;
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
        GameInfo gameInfo = gameTypeAdapter.adapt(dailyGameScore.getGame());
        return new GameScoreData(chatInfo, userInfo, gameInfo, dailyGameScore.getGameSession().getDuration(), dailyGameScore.getPosition(), dailyGameScore.getPoints());
    }
}
