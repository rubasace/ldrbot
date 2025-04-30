package dev.rubasace.linkedin.games.ldrbot.message.config;

import dev.rubasace.linkedin.games.ldrbot.chat.CustomTelegramClient;
import dev.rubasace.linkedin.games.ldrbot.group.GroupNotFoundException;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupService;
import dev.rubasace.linkedin.games.ldrbot.message.GameNameAdapter;
import dev.rubasace.linkedin.games.ldrbot.session.GameNameNotFoundException;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.util.KeyboardMarkupUtils;
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
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.telegram.telegrambots.abilitybots.api.objects.Locality.ALL;
import static org.telegram.telegrambots.abilitybots.api.objects.Privacy.GROUP_ADMIN;

@Component
public class ConfigureAbility extends BaseMessageReplier implements AbilityExtension {

    private final Map<Long, Consumer<Update>> pendingActions;

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigureAbility.class);

    private static final KeyboardMarkupUtils.ButtonData EXIT_ACTION = KeyboardMarkupUtils.ButtonData.of("exit", "Exit Configuration");

    private final CustomTelegramClient customTelegramClient;
    private final TelegramGroupService telegramGroupService;
    private final GameNameAdapter gameNameAdapter;

    public ConfigureAbility(final CustomTelegramClient customTelegramClient, final TelegramGroupService telegramGroupService, final GameNameAdapter gameNameAdapter) {
        super("configure");
        this.pendingActions = new ConcurrentHashMap<>();
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

    public Ability cancel() {
        return Ability.builder()
                      .name("cancel")
                      .info("Cancel current configuration action.")
                      .locality(ALL)
                      .privacy(GROUP_ADMIN)
                      .action(ctx -> cancel(ctx.update().getMessage()))
                      .build();
    }

    private void cancel(final Message message) {
        pendingActions.remove(message.getChatId());
        customTelegramClient.message("Configuration cancelled", message.getChatId());
    }

    protected boolean shouldHandleReply(final Update update) {
        return !isCommand(update) && (pendingActions.containsKey(AbilityUtils.getChatId(update)) || super.shouldHandleReply(update));
    }

    private boolean isCommand(final Update update) {
        return update.hasMessage() && update.getMessage().isCommand();
    }


    private void configure(BaseAbilityBot baseAbilityBot, Update update) {
        Long chatId = AbilityUtils.getChatId(update);
        if (pendingActions.containsKey(chatId)) {
            pendingActions.get(chatId).accept(update);
            return;
        }

        MaybeInaccessibleMessage message = update.getCallbackQuery().getMessage();
        Integer messageId = message.getMessageId();
        String action = getAction(update);
        switch (action) {
            case "tracked-games":
                showTrackedGamesConfig(chatId, messageId);
                return;
            case "timezone":
                showTimezoneConfig(chatId, messageId);
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
                                                                                 KeyboardMarkupUtils.ButtonData.of("tracked-games", "Tracked Games"),
                                                                                 KeyboardMarkupUtils.ButtonData.of("timezone", "Timezone"),
                                                                                 //ConfigAction.of("reminders", "Reminders"),
                                                                                 EXIT_ACTION);

        customTelegramClient.sendOrEditMessage(chatId, "Configuration - Choose an option:", buttons, messageId);

    }


    private void showTrackedGamesConfig(final Long chatId, final Integer messageId) {
        try {
            Set<GameType> trackedGames = telegramGroupService.listTrackedGames(chatId);
            List<KeyboardMarkupUtils.ButtonData> gamesActions = Arrays.stream(GameType.values())
                                                                      .map(gameType -> gameTypeToAction(gameType, trackedGames))
                                                                      .toList();
            KeyboardMarkupUtils.ButtonData[] trackedGamesActions = Stream.concat(gamesActions.stream(),
                                                                                 Stream.of(KeyboardMarkupUtils.ButtonData.of("back", "<< Back to Main Configuration")))
                                                                         .toArray(KeyboardMarkupUtils.ButtonData[]::new);
            InlineKeyboardMarkup buttons = KeyboardMarkupUtils.createTwoColumnLayout(getPrefix(), trackedGamesActions);

            customTelegramClient.editMessage(chatId, messageId, "Configuration — Enable or disable the games tracked in this group", buttons);
        } catch (GroupNotFoundException e) {
            LOGGER.error("Failed to list tracked games for group with id {}", chatId, e);
        }
    }

    private void showTimezoneConfig(final Long chatId, final Integer messageId) {
        this.pendingActions.put(chatId, this::setTimezone);
        customTelegramClient.message("Please send me your timezone (for example: <code>Europe/London</code> or <code>America/New_York</code>):", chatId);
    }

    private KeyboardMarkupUtils.ButtonData gameTypeToAction(final GameType gameType, final Set<GameType> trackedGames) {
        String icon = trackedGames.contains(gameType) ? "✅ " : "❌ ";
        return KeyboardMarkupUtils.ButtonData.of(gameType.name(), icon + StringUtils.capitalize(gameType.name().toLowerCase()));
    }

    private void setTimezone(final Update update) {
        Long chatId = AbilityUtils.getChatId(update);
        try {
            String timeZone = update.getMessage().getText().trim();
            telegramGroupService.setTimezone(chatId, timeZone);
            this.pendingActions.remove(chatId);
        } catch (Exception e) {
            customTelegramClient.errorMessage(
                    "Failed to set timezone, please make sure you send a valid one (e.g. <code>Europe/London</code> or <code>America/New_York</code>).\n" +
                            "Alternatively, You can /cancel to exit", chatId);
        }
    }

}