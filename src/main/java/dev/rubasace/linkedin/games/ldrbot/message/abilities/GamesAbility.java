package dev.rubasace.linkedin.games.ldrbot.message.abilities;

import dev.rubasace.linkedin.games.ldrbot.chat.CustomTelegramClient;
import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupService;
import dev.rubasace.linkedin.games.ldrbot.group.TrackedGamesChangedEvent;
import dev.rubasace.linkedin.games.ldrbot.message.ChatAdapter;
import dev.rubasace.linkedin.games.ldrbot.session.GameInfo;
import dev.rubasace.linkedin.games.ldrbot.session.GameTypeAdapter;
import lombok.SneakyThrows;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.abilitybots.api.objects.Ability;
import org.telegram.telegrambots.abilitybots.api.objects.Locality;
import org.telegram.telegrambots.abilitybots.api.util.AbilityExtension;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Set;
import java.util.stream.Collectors;

import static org.telegram.telegrambots.abilitybots.api.objects.Privacy.PUBLIC;

@Component
public class GamesAbility implements AbilityExtension {

    private final TelegramGroupService telegramGroupService;
    private final CustomTelegramClient customTelegramClient;
    private final ChatAdapter chatAdapter;
    private final GameTypeAdapter gameTypeAdapter;
    private final ApplicationEventPublisher applicationEventPublisher;

    GamesAbility(final TelegramGroupService telegramGroupService, final CustomTelegramClient customTelegramClient, final ChatAdapter chatAdapter, final GameTypeAdapter gameTypeAdapter, final ApplicationEventPublisher applicationEventPublisher) {
        this.telegramGroupService = telegramGroupService;
        this.customTelegramClient = customTelegramClient;
        this.chatAdapter = chatAdapter;
        this.gameTypeAdapter = gameTypeAdapter;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public Ability games() {
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
        Set<GameInfo> trackedGames = telegramGroupService.listTrackedGames(chatInfo).stream()
                                                         .map(gameTypeAdapter::adapt)
                                                         .collect(Collectors.toSet());

        applicationEventPublisher.publishEvent(new TrackedGamesChangedEvent(this, chatInfo.chatId(), trackedGames));
    }

}
