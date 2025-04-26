package dev.rubasace.linkedin.games.ldrbot.message.abilities;

import dev.rubasace.linkedin.games.ldrbot.chat.CustomTelegramClient;
import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupService;
import dev.rubasace.linkedin.games.ldrbot.message.AbilityImplementation;
import dev.rubasace.linkedin.games.ldrbot.message.ChatAdapter;
import dev.rubasace.linkedin.games.ldrbot.message.InvalidUserInputException;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.util.FormatUtils;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.telegram.telegrambots.abilitybots.api.objects.Ability;
import org.telegram.telegrambots.abilitybots.api.objects.Locality;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Set;
import java.util.stream.Collectors;

import static org.telegram.telegrambots.abilitybots.api.objects.Privacy.PUBLIC;

@Component
class GamesAbility implements AbilityImplementation {

    private final TelegramGroupService telegramGroupService;
    private final CustomTelegramClient customTelegramClient;
    private final ChatAdapter chatAdapter;

    GamesAbility(final TelegramGroupService telegramGroupService, final CustomTelegramClient customTelegramClient, final ChatAdapter chatAdapter) {
        this.telegramGroupService = telegramGroupService;
        this.customTelegramClient = customTelegramClient;
        this.chatAdapter = chatAdapter;
    }

    @Override
    public Ability getAbility() {
        return Ability.builder()
                      .name("games")
                      .info("List the games tracked by this group.")
                      .locality(Locality.GROUP)
                      .privacy(PUBLIC)
                      .action(ctx -> listTrackedGames(ctx.update().getMessage()))
                      .build();
    }

    @SneakyThrows
    private void listTrackedGames(final Message message) {
        ChatInfo chatInfo = chatAdapter.adapt(message.getChat());
        Set<GameType> trackedGames = telegramGroupService.listTrackedGames(chatInfo);
        if (CollectionUtils.isEmpty(trackedGames)) {
            throw new InvalidUserInputException("This group is not tracking any games.", chatInfo.chatId());
        } else {
            String text = trackedGames.stream()
                                      .sorted()
                                      .map(game -> "%s %s".formatted(FormatUtils.gameIcon(game), game.name()))
                                      .collect(Collectors.joining("\n"));

            customTelegramClient.message("This group is currently tracking:\n" + text, chatInfo.chatId());
        }
    }

}
