package dev.rubasace.linkedin.games.ldrbot.chat;

import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.summary.GameScoreData;
import dev.rubasace.linkedin.games.ldrbot.summary.GlobalScoreData;
import dev.rubasace.linkedin.games.ldrbot.summary.GroupDailyScore;
import dev.rubasace.linkedin.games.ldrbot.util.FormatUtils;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
class RankingMessageFactory {

    String createRankingMessage(GroupDailyScore groupScore) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>📊 Daily Ranking for %s</b>\n".formatted(FormatUtils.formatDate(groupScore.gameDay())));

        java.util.stream.Stream.of(GameType.values())
                               .sorted(Comparator.comparing(GameType::name))
                               .filter(groupScore.gameScores()::containsKey)
                               .forEach(gameType -> toHtmlGameRanking(gameType, groupScore.gameScores().get(gameType), sb));

        List<GlobalScoreData> global = groupScore.globalScore();

        toHtmlGlobalRanking(sb, global);

        toHtmlFinalMessage(sb, global);

        return sb.toString();
    }

    private void toHtmlGameRanking(final GameType gameType, final List<GameScoreData> scores, final StringBuilder sb) {
        sb.append(toTile(FormatUtils.gameIcon(gameType), gameType.name()));

        for (GameScoreData score : scores) {
            sb.append(formatRankingLine(score.position(), FormatUtils.formatUserMention(score.userInfo()), score.duration(), score.points()));
        }
    }

    private String toTile(final String icon, final String title) {
        return "\n<b><u>%s</u> </b>\n".formatted(title);
    }

    private void toHtmlGlobalRanking(final StringBuilder sb, final List<GlobalScoreData> global) {

        sb.append(toTile("🏆", "Global Score"));

        for (GlobalScoreData scoreData : global) {
            sb.append(formatRankingLine(scoreData.getPosition(), FormatUtils.formatUserMention(scoreData.getUserInfo()), scoreData.getTotalDuration(), scoreData.getPoints()));
        }
    }

    private void toHtmlFinalMessage(final StringBuilder sb, final List<GlobalScoreData> global) {
        List<String> winners = new ArrayList<>();
        for (GlobalScoreData score : global) {
            if (score.getPosition() != 1) {
                break;
            }
            winners.add(FormatUtils.formatUserMention(score.getUserInfo()));
        }
        sb.append("\n<b>🎉🎉🎉 Congratulations ");
        for (int i = 0; i < winners.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("@").append(winners.get(i)).append(" 🏆");
        }
        sb.append(" 🎉🎉🎉</b>\n\n<b>");
        sb.append(winners.size() > 1 ? "You are today's champions" : "You are today's champion");
        sb.append("!</b>\n");
    }

    private String formatRankingLine(int position, String username, Duration duration, int points) {
        String icon = rankingIcon(position);
        String paddedUser = String.format("@%s", username);
        String durationStr = FormatUtils.formatDuration(duration);
        return String.format("%s %s (%s) — %d pts\n", icon, paddedUser, durationStr, points);
    }

    private String rankingIcon(int position) {
        return switch (position) {
            case 1 -> "🥇";
            case 2 -> "🥈";
            case 3 -> "🥉";
            case 4 -> "4️⃣";
            case 5 -> "5️⃣";
            case 6 -> "6️⃣";
            case 7 -> "7️⃣";
            case 8 -> "8️⃣";
            case 9 -> "9️⃣";
            case 10 -> "🔟";
            default -> (position + 1) + ".";
        };
    }
}
