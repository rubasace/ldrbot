package dev.rubasace.linkedin.games.ldrbot.message.config;

import dev.rubasace.linkedin.games.ldrbot.chat.CustomTelegramClient;
import dev.rubasace.linkedin.games.ldrbot.group.GroupNotFoundException;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupService;
import dev.rubasace.linkedin.games.ldrbot.message.GameNameAdapter;
import dev.rubasace.linkedin.games.ldrbot.session.GameNameNotFoundException;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.abilitybots.api.bot.BaseAbilityBot;
import org.telegram.telegrambots.abilitybots.api.objects.Ability;
import org.telegram.telegrambots.abilitybots.api.objects.Locality;
import org.telegram.telegrambots.abilitybots.api.objects.Privacy;
import org.telegram.telegrambots.abilitybots.api.util.AbilityExtension;
import org.telegram.telegrambots.abilitybots.api.util.AbilityUtils;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Component
public class ConfigAbility extends BaseMessageReplier implements AbilityExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigAbility.class);

    private static final ConfigAction EXIT_ACTION = ConfigAction.of("exit", "Exit Configuration");

    private final CustomTelegramClient customTelegramClient;
    private final TelegramGroupService telegramGroupService;
    private final GameNameAdapter gameNameAdapter;

    public ConfigAbility(final CustomTelegramClient customTelegramClient, final TelegramGroupService telegramGroupService, final GameNameAdapter gameNameAdapter) {
        super("configure");
        this.customTelegramClient = customTelegramClient;
        this.telegramGroupService = telegramGroupService;
        this.gameNameAdapter = gameNameAdapter;
    }


    public Ability configure() {
        return Ability.builder()
                      .name("configure")
                      .info("Configure bot settings")
                      .locality(Locality.GROUP)
                      .privacy(Privacy.GROUP_ADMIN)
                      .action(ctx -> showMainConfig(ctx.update()))
                      .reply(this::configure, this::shouldHandleReply)
                      .build();
    }

    private void configure(BaseAbilityBot baseAbilityBot, Update update) {
        MaybeInaccessibleMessage message = update.getCallbackQuery().getMessage();
        Long chatId = AbilityUtils.getChatId(update);
        Integer messageId = message.getMessageId();
        String action = getAction(update);
        switch (action) {
            case "tracked-games":
                showTrackedGamesConfig(chatId, messageId);
                return;
            case "back":
                showMainConfig(chatId, messageId);
                return;
            case "exit":
                customTelegramClient.deleteMessage(chatId, messageId);
                return;
        }
        try {
            GameType gameType = gameNameAdapter.adapt(update.getCallbackQuery().getData().substring(getPrefix().length()), chatId);
            toggleGameTracking(gameType, chatId, messageId);
        } catch (GameNameNotFoundException e) {
            //ignore, adapter will only work when receiving a click on a game button for toggling tracking
        }
    }

    private void toggleGameTracking(final GameType gameType, final Long chatId, final Integer messageId) {
        try {
            telegramGroupService.toggleGameTracking(chatId, gameType);
            showTrackedGamesConfig(chatId, messageId);
        } catch (GroupNotFoundException e) {
            LOGGER.error("Group not found", e);
        }
    }

    private void showMainConfig(final Update update) {
        showMainConfig(AbilityUtils.getChatId(update), null);
    }

    private void showMainConfig(final Long chatId, final Integer messageId) {
        InlineKeyboardMarkup buttons = KeyboardMarkupUtils.createTwoColumnLayout(getPrefix(),
                                                                                 ConfigAction.of("tracked-games", "Tracked Games"),
                                                                                 //ConfigAction.of("timezone", "Timezone"),
                                                                                 //ConfigAction.of("reminders", "Reminders"),
                                                                                 EXIT_ACTION);

        customTelegramClient.sendOrEditMessage(chatId, "Configuration - Choose an option:", buttons, messageId);

    }


    private void showTrackedGamesConfig(final Long chatId, final Integer messageId) {
        try {
            Set<GameType> trackedGames = telegramGroupService.listTrackedGames(chatId);
            List<ConfigAction> gamesActions = Arrays.stream(GameType.values())
                                                    .map(gameType -> gameTypeToAction(gameType, trackedGames))
                                                    .toList();
            ConfigAction[] trackedGamesActions = Stream.concat(gamesActions.stream(), Stream.of(ConfigAction.of("back", "<< Back to Main Configuration")))
                                                       .toArray(ConfigAction[]::new);
            InlineKeyboardMarkup buttons = KeyboardMarkupUtils.createTwoColumnLayout(getPrefix(), trackedGamesActions);

            customTelegramClient.editMessage(chatId, messageId, "Configuration — Enable or disable the games tracked in this group", buttons);
        } catch (GroupNotFoundException e) {
            LOGGER.error("Failed to list tracked games for group with id {}", chatId, e);
        }
    }

    private ConfigAction gameTypeToAction(final GameType gameType, final Set<GameType> trackedGames) {
        String icon = trackedGames.contains(gameType) ? "✅ " : "❌";
        return ConfigAction.of(gameType.name(), icon + StringUtils.capitalize(gameType.name().toLowerCase()));
    }

}