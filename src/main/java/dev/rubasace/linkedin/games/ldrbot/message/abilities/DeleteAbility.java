package dev.rubasace.linkedin.games.ldrbot.message.abilities;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.rubasace.linkedin.games.ldrbot.chat.CustomTelegramClient;
import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.group.GroupNotFoundException;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupService;
import dev.rubasace.linkedin.games.ldrbot.message.ChatAdapter;
import dev.rubasace.linkedin.games.ldrbot.message.GameNameAdapter;
import dev.rubasace.linkedin.games.ldrbot.message.InvalidUserInputException;
import dev.rubasace.linkedin.games.ldrbot.message.UserAdapter;
import dev.rubasace.linkedin.games.ldrbot.message.config.BaseMessageReplier;
import dev.rubasace.linkedin.games.ldrbot.session.GameSessionService;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import dev.rubasace.linkedin.games.ldrbot.util.InputSanitizer;
import dev.rubasace.linkedin.games.ldrbot.util.KeyboardMarkupUtils;
import dev.rubasace.linkedin.games.ldrbot.util.LinkedinTimeUtils;
import dev.rubasace.linkedin.games.ldrbot.util.UsageFormatUtils;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.abilitybots.api.bot.BaseAbilityBot;
import org.telegram.telegrambots.abilitybots.api.objects.Ability;
import org.telegram.telegrambots.abilitybots.api.objects.Locality;
import org.telegram.telegrambots.abilitybots.api.util.AbilityExtension;
import org.telegram.telegrambots.abilitybots.api.util.AbilityUtils;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.telegram.telegrambots.abilitybots.api.objects.Privacy.PUBLIC;

@Component
public class DeleteAbility extends BaseMessageReplier implements AbilityExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteAbility.class);

    public static final String INVALID_ARGUMENTS_MESSAGE = """
            This command only works with an interactive menu.
            
            Use <code>/delete</code> to choose a game from the menu.
            """;
    
    private final GameSessionService gameSessionService;
    private final ChatAdapter chatAdapter;
    private final UserAdapter userAdapter;
    private final GameNameAdapter gameNameAdapter;
    private final CustomTelegramClient customTelegramClient;
    private final TelegramGroupService telegramGroupService;
    private final Cache<String, Long> flowOwners; // key: chatId:userId, value: userId (flow ownership)

    DeleteAbility(final GameSessionService gameSessionService, 
                  final ChatAdapter chatAdapter, 
                  final UserAdapter userAdapter, 
                  final GameNameAdapter gameNameAdapter,
                  final CustomTelegramClient customTelegramClient,
                  final TelegramGroupService telegramGroupService) {
        super("delete");
        this.gameSessionService = gameSessionService;
        this.chatAdapter = chatAdapter;
        this.userAdapter = userAdapter;
        this.gameNameAdapter = gameNameAdapter;
        this.customTelegramClient = customTelegramClient;
        this.telegramGroupService = telegramGroupService;
        this.flowOwners = Caffeine.newBuilder()
                                  .expireAfterWrite(10, TimeUnit.MINUTES)
                                  .maximumSize(10_000)
                                  .build();
    }

    public Ability delete() {
        return Ability.builder()
                      .name("delete")
                      .info(UsageFormatUtils.formatUsage("/delete", "Remove your submitted time for a game using an interactive menu."))
                      .locality(Locality.GROUP)
                      .privacy(PUBLIC)
                      .action(ctx -> handleDeleteCommand(ctx.update().getMessage(), InputSanitizer.sanitizeArguments(ctx.arguments())))
                      .reply(this::handleCallback, this::shouldHandleReply)
                      .build();
    }

    @SneakyThrows
    private void handleDeleteCommand(final Message message, final String[] arguments) {
        // Only accept no arguments - guided flow only
        if (arguments.length > 0) {
            throw new InvalidUserInputException(INVALID_ARGUMENTS_MESSAGE, message.getChatId());
        }
        
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();
        
        // Store flow ownership keyed by chatId:userId (not messageId)
        // This prevents race conditions where any user could click first
        flowOwners.put(chatId + ":" + userId, userId);
        
        // Show guided keyboard flow
        showGameSelection(chatId, userId, null);
    }

    private void showGameSelection(Long chatId, Long userId, Integer messageId) {
        try {
            Set<GameType> trackedGames = telegramGroupService.listTrackedGames(chatId);
            
            if (trackedGames.isEmpty()) {
                customTelegramClient.sendMessage("No games are currently tracked in this group. Use /configure to add games.", chatId);
                return;
            }
            
            KeyboardMarkupUtils.ButtonData[] gameButtons = trackedGames.stream()
                    .map(gameType -> KeyboardMarkupUtils.ButtonData.of(
                            gameType.name(),
                            StringUtils.capitalize(gameType.name().toLowerCase())
                    ))
                    .toArray(KeyboardMarkupUtils.ButtonData[]::new);
            
            InlineKeyboardMarkup keyboard = KeyboardMarkupUtils.createTwoColumnLayout(getPrefix(), gameButtons);
            
            // Send or edit the message
            if (messageId == null) {
                customTelegramClient.sendMessage(chatId, "Select the game to delete your record for today:", keyboard);
            } else {
                customTelegramClient.editMessage(chatId, messageId, "Select the game to delete your record for today:", keyboard);
            }
            
        } catch (GroupNotFoundException e) {
            LOGGER.error("Group not found for chatId: {}", chatId, e);
            customTelegramClient.sendErrorMessage("Failed to load tracked games.", chatId);
        }
    }

    private void handleCallback(BaseAbilityBot bot, Update update) {
        if (!update.hasCallbackQuery()) {
            return;
        }
        
        Long chatId = AbilityUtils.getChatId(update);
        Long callbackUserId = update.getCallbackQuery().getFrom().getId();
        MaybeInaccessibleMessage message = update.getCallbackQuery().getMessage();
        Integer messageId = message.getMessageId();
        String callbackQueryId = update.getCallbackQuery().getId();
        
        // Verify user authorization using chatId:userId key (not messageId)
        String flowKey = chatId + ":" + callbackUserId;
        Long flowOwnerId = flowOwners.getIfPresent(flowKey);
        
        if (flowOwnerId == null || !isAuthorizedUser(update, flowOwnerId)) {
            customTelegramClient.answerCallbackQuery(callbackQueryId, "This menu is not for you.", false);
            return;
        }
        
        // Get the selected game
        String gameAction = getAction(update);
        
        try {
            GameType gameType = GameType.valueOf(gameAction);
            ChatInfo chatInfo = new ChatInfo(chatId, null, true);
            UserInfo userInfo = userAdapter.adapt(update.getCallbackQuery().getFrom());
            
            // Delete the session
            gameSessionService.deleteDaySession(chatInfo, userInfo, gameType, LinkedinTimeUtils.todayGameDay());
            
            // Answer callback query and delete the menu message
            customTelegramClient.answerCallbackQuery(callbackQueryId);
            customTelegramClient.deleteMessage(chatId, messageId);
            
            // Clean up cache
            flowOwners.invalidate(flowKey);
            
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid game type selected: {}", gameAction, e);
            customTelegramClient.answerCallbackQuery(callbackQueryId, "Invalid game selection.", true);
        } catch (Exception e) {
            LOGGER.error("Error deleting game session", e);
            customTelegramClient.answerCallbackQuery(callbackQueryId, "Failed to delete session.", true);
        }
    }
}
