package dev.rubasace.linkedin.games.ldrbot.message.abilities;

import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.group.GroupNotFoundException;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroup;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupAdapter;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupService;
import dev.rubasace.linkedin.games.ldrbot.ranking.GroupRankingService;
import dev.rubasace.linkedin.games.ldrbot.util.LinkedinTimeUtils;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.abilitybots.api.objects.Ability;
import org.telegram.telegrambots.abilitybots.api.objects.Locality;
import org.telegram.telegrambots.abilitybots.api.util.AbilityExtension;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Optional;

import static org.telegram.telegrambots.abilitybots.api.objects.Privacy.PUBLIC;

@Component
public class RankingAbility implements AbilityExtension {

    private final TelegramGroupService telegramGroupService;
    private final GroupRankingService groupRankingService;
    private final TelegramGroupAdapter telegramGroupAdapter;

    RankingAbility(final TelegramGroupService telegramGroupService, final GroupRankingService groupRankingService, final TelegramGroupAdapter telegramGroupAdapter) {
        this.telegramGroupService = telegramGroupService;
        this.groupRankingService = groupRankingService;
        this.telegramGroupAdapter = telegramGroupAdapter;
    }


    public Ability ranking() {
        return Ability.builder()
                      .name("ranking")
                      .info("Calculate and show today’s group leaderboard.")
                      .locality(Locality.GROUP)
                      .privacy(PUBLIC)
                      .action(ctx -> dailyRanking(ctx.update().getMessage()))
                      .build();
    }

    @SneakyThrows
    private void dailyRanking(final Message message) {
        Optional<TelegramGroup> telegramGroup = telegramGroupService.findGroup(message.getChat().getId());
        if (telegramGroup.isEmpty()) {
            return;
        }
        dailyRanking(telegramGroup.get());
    }

    private void dailyRanking(@NotNull TelegramGroup telegramGroup) throws GroupNotFoundException {
        ChatInfo chatInfo = telegramGroupAdapter.adapt(telegramGroup);
        groupRankingService.createDailyRanking(chatInfo, LinkedinTimeUtils.todayGameDay());
    }

}
