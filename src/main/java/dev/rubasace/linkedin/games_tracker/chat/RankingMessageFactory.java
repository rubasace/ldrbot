package dev.rubasace.linkedin.games_tracker.chat;

import dev.rubasace.linkedin.games_tracker.session.GameType;
import dev.rubasace.linkedin.games_tracker.summary.GameScoreData;
import dev.rubasace.linkedin.games_tracker.summary.GlobalScoreData;
import dev.rubasace.linkedin.games_tracker.summary.GroupDailyScore;
import dev.rubasace.linkedin.games_tracker.util.FormatUtils;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
class RankingMessageFactory {

    String createRankingMessage(GroupDailyScore groupScore) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>📊 Daily Ranking for %s</b>\n".formatted(FormatUtils.formatDate(groupScore.date())));

        for (Map.Entry<GameType, List<GameScoreData>> entry : groupScore.gameScores().entrySet()) {
            toHtmlGameRanking(entry.getKey(), entry.getValue(), sb);
        }

        List<GlobalScoreData> global = groupScore.globalScore();

        toHtmlGlobalRanking(sb, global);

        toHtmlFinalMessage(groupScore.winners(), sb);

        return sb.toString();
    }

    private void toHtmlGameRanking(final GameType gameType, final List<GameScoreData> scores, final StringBuilder sb) {
        sb.append(toTile(FormatUtils.gameIcon(gameType), gameType.name()));

        for (int i = 0; i < scores.size(); i++) {
            GameScoreData score = scores.get(i);
            sb.append(formatRankingLine(i, score.username(), score.duration(), score.points()));
        }
    }

    private String toTile(final String icon, final String title) {
        return "\n<b><u>%s</u> </b>\n".formatted(title);
    }

    private void toHtmlGlobalRanking(final StringBuilder sb, final List<GlobalScoreData> global) {

        sb.append(toTile("🏆", "Global Score"));

        for (int i = 0; i < global.size(); i++) {
            GlobalScoreData score = global.get(i);
            sb.append(formatRankingLine(i, score.username(), score.totalDuration(), score.points()));
        }
    }

    //TODO allow admin to make message configurable?
    private void toHtmlFinalMessage(final List<String> winners, final StringBuilder sb) {
        sb.append("\n<b>🎉🎉🎉 Congratulations @%s, you are today's champion%s 🏆! 🎉🎉🎉</b>"
                          .formatted(String.join(" and @", winners), winners.size() > 1 ? "s" : ""));
    }

    private String formatRankingLine(int position, String username, Duration duration, int points) {
        String icon = rankingIcon(position, points);
        String paddedUser = String.format("@%s", username);
        String durationStr = FormatUtils.formatDuration(duration);
        return String.format("%s %s (%s) — %d pts\n", icon, paddedUser, durationStr, points);
    }

    //FIXME improve this so if there are two winners they both get the same icon, same for seconds and thirds (use same approach as winners probably)
    private String rankingIcon(int position, final int points) {
        if (points == 3) {
            return "🥇";
        }
        if (points == 2) {
            return "🥈";
        }
        if (points == 1) {
            return "🥉";
        }
        return switch (position) {
            case 0 -> "🥇";
            case 1 -> "🥈";
            case 2 -> "🥉";
            case 3 -> "4️⃣";
            case 4 -> "5️⃣";
            case 5 -> "6️⃣";
            case 6 -> "7️⃣";
            case 7 -> "8️⃣";
            case 8 -> "9️⃣";
            case 9 -> "🔟";
            default -> (position + 1) + ".";
        };
    }
}
