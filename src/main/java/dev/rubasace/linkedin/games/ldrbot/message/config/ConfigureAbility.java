package dev.rubasace.linkedin.games.ldrbot.message.config;

import dev.rubasace.linkedin.games.ldrbot.chat.CustomTelegramClient;
import dev.rubasace.linkedin.games.ldrbot.configuration.TelegramBotProperties;
import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.group.GroupNotFoundException;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroup;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupService;
import dev.rubasace.linkedin.games.ldrbot.message.GameNameAdapter;
import dev.rubasace.linkedin.games.ldrbot.session.GameNameNotFoundException;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUser;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUserService;
import dev.rubasace.linkedin.games.ldrbot.util.FormatUtils;
import dev.rubasace.linkedin.games.ldrbot.util.KeyboardMarkupUtils;
import dev.rubasace.linkedin.games.ldrbot.util.LinkedinTimeUtils;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private static final KeyboardMarkupUtils.ButtonData EXIT_BUTTON = KeyboardMarkupUtils.ButtonData.of("exit", "Exit Configuration");

    private static final String PLAYER_ACTION_PREFIX = "player-";

    private static final String PLAYERS_TEXT = """
            Configuration — Players
            ✅ taking part   ⏸️ parked

            Tap a player to park them or bring them back. The bot does not wait for a parked player's result, does not remind them, and does not count them when awarding points. They come back automatically the next time they submit a result.""";

    private static final String NO_PLAYERS_TEXT = """
            Configuration — Players

            Nobody has played in this group yet. Players appear here once they have posted in the chat.""";

    private static final String NOT_GROUP_ADMIN_ANSWER = "Only group admins can manage players.";
    private static final String NOW_TAKING_PART_ANSWER = "%s is now taking part";
    private static final String NOW_PARKED_ANSWER = "%s is now parked";
    private static final String NO_LONGER_A_MEMBER_ANSWER = "%s is no longer in this group.";
    private static final String TOGGLE_FAILED_ANSWER = "Couldn't update that player. Please try again.";

    private final CustomTelegramClient customTelegramClient;
    private final TelegramGroupService telegramGroupService;
    private final TelegramUserService telegramUserService;
    private final GameNameAdapter gameNameAdapter;
    private final TelegramBotProperties telegramBotProperties;

    public ConfigureAbility(final CustomTelegramClient customTelegramClient, final TelegramGroupService telegramGroupService,
                            final TelegramUserService telegramUserService, final GameNameAdapter gameNameAdapter,
                            final TelegramBotProperties telegramBotProperties) {
        super("configure");
        this.pendingActions = new ConcurrentHashMap<>();
        this.customTelegramClient = customTelegramClient;
        this.telegramGroupService = telegramGroupService;
        this.telegramUserService = telegramUserService;
        this.gameNameAdapter = gameNameAdapter;
        this.telegramBotProperties = telegramBotProperties;
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
        customTelegramClient.sendMessage("Configuration cancelled", message.getChatId());
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
            case "players":
                if (denyNonGroupAdmin(baseAbilityBot, update)) {
                    return;
                }
                customTelegramClient.answerCallbackQuery(update.getCallbackQuery().getId());
                showPlayersConfig(chatId, messageId);
                return;
            case "toggle-read-messages":
                toggleReadFromMessages(chatId, messageId);
                return;
            case "back":
                showMainConfig(chatId, messageId);
                return;
            case "exit":
                customTelegramClient.deleteMessage(chatId, messageId);
                return;
        }
        if (action.startsWith(PLAYER_ACTION_PREFIX)) {
            if (denyNonGroupAdmin(baseAbilityBot, update)) {
                return;
            }
            togglePlayerParticipation(chatId, messageId, action, update.getCallbackQuery().getId());
            return;
        }
        try {
            GameType gameType = gameNameAdapter.adapt(update.getCallbackQuery().getData().substring(getPrefix().length()), chatId);
            toggleGameTracking(gameType, chatId, messageId);
        } catch (GameNameNotFoundException e) {
            //ignore, adapter will only work when receiving a click on a game button for toggling tracking
        }
    }

    /**
     * Refuses a tap on the player entries to anybody who is not a Telegram admin of the group, answering the callback
     * with the reason and changing nothing. It runs on the tapping user and on the raw {@code player-} prefix, before
     * any id is parsed: callback data is chosen by the client, not by the button, so parsing first would put
     * unvalidated bytes into {@link Long#parseLong} ahead of the authorisation decision, and the resulting
     * {@link NumberFormatException} would escape onto the framework's single update thread.
     * <p>
     * It is fail-closed. {@code isGroupAdmin} resolves the administrator list through the silent sender and falls back
     * to an empty one, so a failed Bot API call — or a callback delivered in a private chat, where asking for group
     * administrators is an error — refuses the tap.
     */
    private boolean denyNonGroupAdmin(final BaseAbilityBot baseAbilityBot, final Update update) {
        if (baseAbilityBot.isGroupAdmin(update, update.getCallbackQuery().getFrom().getId())) {
            return false;
        }
        customTelegramClient.answerCallbackQuery(update.getCallbackQuery().getId(), NOT_GROUP_ADMIN_ANSWER, false);
        return true;
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
                                                                                 KeyboardMarkupUtils.ButtonData.of("players", "Players"),
//                                                                                 KeyboardMarkupUtils.ButtonData.of("toggle-read-messages", "Read Messages"),
                                                                                 EXIT_BUTTON);

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

    private void showPlayersConfig(final Long chatId, final Integer messageId) {
        try {
            drawPlayers(chatId, messageId, listMembers(chatId), listPlayersParkedToday(chatId));
        } catch (Exception e) {
            LOGGER.error("Failed to show the player list for group with id {}", chatId, e);
        }
    }

    /**
     * Parks the tapped player or brings them back, then tells the admin what happened and redraws the list in place.
     * <p>
     * The callback is answered <em>exactly once on every tap that names a player, before the redraw is attempted</em>,
     * and never only on a successful toggle. Answering first cannot break the redraw, because
     * {@code answerCallbackQuery} logs its own failures and never throws, and it is the only call that clears the
     * tapping client's spinner. Answering afterwards would lose the toast on the path it exists for: when a second
     * admin has already applied the same change, the redraw is byte-identical to what this client is showing, Telegram
     * answers {@code 400 message is not modified} and {@code editMessage} rethrows.
     * <p>
     * The text is the state the redrawn list will show for that player, so the losing side of two simultaneous taps is
     * told the state that actually holds rather than the one they asked for.
     */
    private void togglePlayerParticipation(final Long chatId, final Integer messageId, final String action, final String callbackQueryId) {
        Long userId;
        try {
            userId = Long.parseLong(action.substring(PLAYER_ACTION_PREFIX.length()));
        } catch (NumberFormatException e) {
            //ignore, only a hand-crafted callback carries a suffix that is not one of the player ids we drew
            return;
        }

        Set<TelegramUser> members;
        Set<Long> parkedPlayers;
        Optional<String> answer;
        try {
            telegramGroupService.togglePlayerParticipation(chatId, userId);
            members = listMembers(chatId);
            parkedPlayers = listPlayersParkedToday(chatId);
            answer = toggleAnswer(members, parkedPlayers, userId);
        } catch (Exception e) {
            LOGGER.error("Failed to toggle the participation of player {} in group with id {}", userId, chatId, e);
            customTelegramClient.answerCallbackQuery(callbackQueryId, TOGGLE_FAILED_ANSWER, false);
            return;
        }
        //an empty answer is the id naming nobody this bot knows: no player to report on, and nothing was changed
        answer.ifPresent(text -> {
            customTelegramClient.answerCallbackQuery(callbackQueryId, text, false);
            try {
                drawPlayers(chatId, messageId, members, parkedPlayers);
            } catch (Exception e) {
                LOGGER.error("Failed to redraw the player list for group with id {}", chatId, e);
            }
        });
    }

    private Optional<String> toggleAnswer(final Set<TelegramUser> members, final Set<Long> parkedPlayers, final Long userId) {
        Optional<TelegramUser> member = members.stream()
                                               .filter(telegramUser -> telegramUser.getId().equals(userId))
                                               .findFirst();
        // A player who left the group between the list being drawn and the button being tapped is no longer a member,
        // so the toggle was a no-op, but they are still a user this bot can name. Empty carries the remaining case —
        // the id names nobody this bot knows — so the caller needs no sentinel to tell the two apart.
        Optional<String> displayName = member.or(() -> telegramUserService.find(userId))
                                             .map(FormatUtils::formatUserName);
        if (member.isEmpty()) {
            return displayName.map(NO_LONGER_A_MEMBER_ANSWER::formatted);
        }
        String answerTemplate = parkedPlayers.contains(userId) ? NOW_PARKED_ANSWER : NOW_TAKING_PART_ANSWER;
        return displayName.map(answerTemplate::formatted);
    }

    private void drawPlayers(final Long chatId, final Integer messageId, final Set<TelegramUser> members, final Set<Long> parkedPlayers) {
        KeyboardMarkupUtils.ButtonData[] playerActions = members.stream()
                                                                .filter(telegramUser -> !telegramBotProperties.getUsername().equals(telegramUser.getUserName()))
                                                                .sorted(Comparator.comparing(FormatUtils::formatUserName))
                                                                .map(telegramUser -> playerToAction(telegramUser, parkedPlayers))
                                                                .toArray(KeyboardMarkupUtils.ButtonData[]::new);

        customTelegramClient.editMessage(chatId, messageId, playerActions.length == 0 ? NO_PLAYERS_TEXT : PLAYERS_TEXT, playersKeyboard(playerActions));
    }

    private KeyboardMarkupUtils.ButtonData playerToAction(final TelegramUser telegramUser, final Set<Long> parkedPlayers) {
        String icon = parkedPlayers.contains(telegramUser.getId()) ? "⏸️ " : "✅ ";
        return KeyboardMarkupUtils.ButtonData.of(PLAYER_ACTION_PREFIX + telegramUser.getId(), icon + FormatUtils.formatUserName(telegramUser));
    }

    /**
     * The players in two columns, with the Back button on a full-width row of its own at every player count.
     * {@link KeyboardMarkupUtils#createLayout} starts a new row on every second button, so appending Back as an
     * ordinary element would leave it at half width beside the last player whenever the group has an odd number of
     * them, and a mis-tap there parks somebody and announces it to the whole group.
     * <p>
     * With no players the keyboard is exactly that one row: the two-column layout is skipped altogether, because it
     * seeds an empty first row before placing anything and would otherwise ship a blank row above Back.
     */
    private InlineKeyboardMarkup playersKeyboard(final KeyboardMarkupUtils.ButtonData[] playerActions) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        if (playerActions.length > 0) {
            rows.addAll(KeyboardMarkupUtils.createTwoColumnLayout(getPrefix(), playerActions).getKeyboard());
        }
        InlineKeyboardRow backRow = new InlineKeyboardRow();
        backRow.add(InlineKeyboardButton.builder().text("<< Back to Main Configuration").callbackData(getPrefix() + "back").build());
        rows.add(backRow);
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private Set<TelegramUser> listMembers(final Long chatId) throws GroupNotFoundException {
        return telegramGroupService.findMembers(new ChatInfo(chatId, null, true));
    }

    private Set<Long> listPlayersParkedToday(final Long chatId) throws GroupNotFoundException {
        return telegramGroupService.listPlayersParkedOn(chatId, LinkedinTimeUtils.todayGameDay());
    }

    private void showTimezoneConfig(final Long chatId, final Integer messageId) {
        this.pendingActions.put(chatId, this::setTimezone);
        customTelegramClient.sendMessage("Please send me your timezone (for example: <code>Europe/London</code> or <code>America/New_York</code>):", chatId);
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
            customTelegramClient.sendErrorMessage(
                    "Failed to set timezone, please make sure you send a valid one (e.g. <code>Europe/London</code> or <code>America/New_York</code>).\n" +
                            "Alternatively, You can /cancel to exit", chatId);
        }
    }

    private void showReadFromMessagesConfig(final Long chatId, final Integer messageId) {
        boolean currentSetting = telegramGroupService.findGroup(chatId)
                .map(TelegramGroup::isReadFromMessages)
                .orElse(false);

        InlineKeyboardMarkup buttons = KeyboardMarkupUtils.createTwoColumnLayout(getPrefix(),
                KeyboardMarkupUtils.ButtonData.of("toggle-read-messages", currentSetting ? "Disable" : "Enable"),
                KeyboardMarkupUtils.ButtonData.of("back", "<< Back to Main Configuration"));

        customTelegramClient.sendOrEditMessage(chatId, "Read From Messages: " + (currentSetting ? "Enabled" : "Disabled"), buttons, messageId);
    }

    private void toggleReadFromMessages(final Long chatId, final Integer messageId) {
        try {
            boolean currentSetting = telegramGroupService.findGroup(chatId)
                                                          .map(TelegramGroup::isReadFromMessages)
                                                          .orElse(false);
            telegramGroupService.setReadFromMessages(chatId, !currentSetting);
            showReadFromMessagesConfig(chatId, messageId);
        } catch (GroupNotFoundException e) {
            customTelegramClient.sendErrorMessage("Group not found. Unable to toggle Read From Messages.", chatId);
        }
    }
}