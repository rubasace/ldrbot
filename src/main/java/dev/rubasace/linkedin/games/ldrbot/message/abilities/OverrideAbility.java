package dev.rubasace.linkedin.games.ldrbot.message.abilities;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.rubasace.linkedin.games.ldrbot.chat.CustomTelegramClient;
import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.group.GroupNotFoundException;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroup;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupService;
import dev.rubasace.linkedin.games.ldrbot.message.ChatAdapter;
import dev.rubasace.linkedin.games.ldrbot.message.GameNameAdapter;
import dev.rubasace.linkedin.games.ldrbot.message.InvalidUserInputException;
import dev.rubasace.linkedin.games.ldrbot.message.UserAdapter;
import dev.rubasace.linkedin.games.ldrbot.message.config.BaseMessageReplier;
import dev.rubasace.linkedin.games.ldrbot.session.*;
import dev.rubasace.linkedin.games.ldrbot.user.*;
import dev.rubasace.linkedin.games.ldrbot.util.*;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.abilitybots.api.bot.BaseAbilityBot;
import org.telegram.telegrambots.abilitybots.api.objects.Ability;
import org.telegram.telegrambots.abilitybots.api.objects.Locality;
import org.telegram.telegrambots.abilitybots.api.objects.Privacy;
import org.telegram.telegrambots.abilitybots.api.util.AbilityExtension;
import org.telegram.telegrambots.abilitybots.api.util.AbilityUtils;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import dev.rubasace.linkedin.games.ldrbot.configuration.TelegramBotProperties;


@Component
public class OverrideAbility extends BaseMessageReplier implements AbilityExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(OverrideAbility.class);

    public static final String INVALID_TIME_FORMAT_MESSAGE = """
            Invalid time format.
            
            Use <code>mm:ss</code>, e.g. <code>1:30</code> or <code>12:05</code>
            """;

    private final TelegramUserService telegramUserService;
    private final GameSessionService gameSessionService;
    private final CustomTelegramClient customTelegramClient;
    private final ChatAdapter chatAdapter;
    private final UserAdapter userAdapter;
    private final TelegramUserAdapter telegramUserAdapter;
    private final GameNameAdapter gameNameAdapter;
    private final GameTypeAdapter gameTypeAdapter;
    private final TelegramGroupService telegramGroupService;
    private final TelegramBotProperties telegramBotProperties;
    private final Cache<Integer, Message> messageCache;
    private final Cache<String, FlowState> flowStates; // key: chatId:userId

    OverrideAbility(final TelegramUserService telegramUserService, 
                   final GameSessionService gameSessionService, 
                   final CustomTelegramClient customTelegramClient, 
                   final ChatAdapter chatAdapter, 
                   final UserAdapter userAdapter, 
                   final TelegramUserAdapter telegramUserAdapter, 
                   final GameNameAdapter gameNameAdapter, 
                   final GameTypeAdapter gameTypeAdapter,
                   final TelegramGroupService telegramGroupService,
                   final TelegramBotProperties telegramBotProperties) {
        super("override");
        this.telegramUserService = telegramUserService;
        this.gameSessionService = gameSessionService;
        this.customTelegramClient = customTelegramClient;
        this.chatAdapter = chatAdapter;
        this.userAdapter = userAdapter;
        this.telegramUserAdapter = telegramUserAdapter;
        this.gameNameAdapter = gameNameAdapter;
        this.gameTypeAdapter = gameTypeAdapter;
        this.telegramGroupService = telegramGroupService;
        this.telegramBotProperties = telegramBotProperties;
        this.messageCache = Caffeine.newBuilder()
                                    .expireAfterWrite(10, TimeUnit.MINUTES)
                                    .maximumSize(10_000)
                                    .build();
        this.flowStates = Caffeine.newBuilder()
                                  .expireAfterWrite(10, TimeUnit.MINUTES)
                                  .maximumSize(10_000)
                                  .build();
    }

    public Ability override() {
        return Ability.builder()
                      .name("override")
                .info("Manually set or update a user's result (admin-only). Use without arguments for a guided flow.")
                      .locality(Locality.GROUP)
                      .privacy(Privacy.GROUP_ADMIN)
                      .reply(this::handleReply, this::shouldHandleReply)
                      .action(ctx -> handleOverrideCommand(ctx.update().getMessage(), InputSanitizer.sanitizeArguments(ctx.arguments())))
                      .build();
    }

    private void handleReply(BaseAbilityBot bot, Update update) {
        if (update.hasCallbackQuery()) {
            handleCallback(update);
        }
    }

    @SneakyThrows
    private void handleOverrideCommand(final Message message, final String[] arguments) {
        startGuidedFlow(message);
    }

    private void startGuidedFlow(Message message) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();
        String flowKey = chatId + ":" + userId;
        
        FlowState state = new FlowState();
        state.setChatId(chatId);
        state.setInitiatorUserId(userId);
        state.setCurrentStep(FlowStep.USER_SELECTION);
        flowStates.put(flowKey, state);
        
        showUserSelection(state, null);
    }

    private void showUserSelection(FlowState state, Integer messageId) {
        try {
            Set<TelegramUser> members = telegramGroupService.findMembers(new ChatInfo(state.getChatId(), null, true));

            if (members.isEmpty()) {
                customTelegramClient.sendMessage("No members found in this group.", state.getChatId());
                cleanupFlow(state);
                return;
            }
            
            KeyboardMarkupUtils.ButtonData[] userButtons = members.stream()
                    .filter(user -> !user.getUserName().equals(telegramBotProperties.getUsername()))
                    .sorted(Comparator.comparing(TelegramUser::getUserName))
                    .map(user -> KeyboardMarkupUtils.ButtonData.of(
                            "user-" + user.getId(),
                            user.getUserName()
                    ))
                    .toArray(KeyboardMarkupUtils.ButtonData[]::new);

            InlineKeyboardMarkup keyboard = KeyboardMarkupUtils.createTwoColumnLayoutWithCancel(getPrefix(), userButtons);
            String text = "Select the user";
            
            if (messageId == null) {
                customTelegramClient.sendMessage(state.getChatId(), text, keyboard);
            } else {
                customTelegramClient.editMessage(state.getChatId(), messageId, text, keyboard);
                state.setMessageId(messageId);
            }
        } catch (GroupNotFoundException e) {
            LOGGER.error("Group not found: {}", state.getChatId(), e);
            customTelegramClient.sendErrorMessage("Failed to load group members.", state.getChatId());
            cleanupFlow(state);
        }
    }

    private void showGameSelection(FlowState state) {
        try {
            Set<GameType> trackedGames = telegramGroupService.listTrackedGames(state.getChatId());
            
            if (trackedGames.isEmpty()) {
                customTelegramClient.editMessage(
                    state.getChatId(), 
                    state.getMessageId(), 
                    "No games are currently tracked in this group."
                );
                cleanupFlow(state);
                return;
            }
            
            KeyboardMarkupUtils.ButtonData[] gameButtons = trackedGames.stream()
                    .map(gameType -> KeyboardMarkupUtils.ButtonData.of(
                            "game-" + gameType.name(),
                            StringUtils.capitalize(gameType.name().toLowerCase())
                    ))
                    .toArray(KeyboardMarkupUtils.ButtonData[]::new);

            InlineKeyboardMarkup keyboard = KeyboardMarkupUtils.createTwoColumnLayoutWithCancel(getPrefix(), gameButtons);
            customTelegramClient.editMessage(
                state.getChatId(), 
                state.getMessageId(),
                    "Select the game",
                keyboard
            );
        } catch (GroupNotFoundException e) {
            LOGGER.error("Group not found: {}", state.getChatId(), e);
            customTelegramClient.editMessage(
                state.getChatId(), 
                state.getMessageId(), 
                "Failed to load tracked games."
            );
            cleanupFlow(state);
        }
    }

    private void showMinuteSelection(FlowState state) {
        state.setCurrentStep(FlowStep.MINUTE_SELECTION);

        KeyboardMarkupUtils.ButtonData[] minuteButtons = IntStream.rangeClosed(0, 29)
                .mapToObj(m -> KeyboardMarkupUtils.ButtonData.of("minute-" + m, String.format("%02d", m)))
                .toArray(KeyboardMarkupUtils.ButtonData[]::new);

        InlineKeyboardMarkup keyboard = KeyboardMarkupUtils.createLayoutWithCancel(getPrefix(), 6, minuteButtons);
        customTelegramClient.editMessage(
                state.getChatId(),
                state.getMessageId(),
                "Select the minutes",
                keyboard
        );
    }

    private void showSecondSelection(FlowState state) {
        state.setCurrentStep(FlowStep.SECOND_SELECTION);

        KeyboardMarkupUtils.ButtonData[] secondButtons = IntStream.rangeClosed(0, 59)
                .mapToObj(s -> KeyboardMarkupUtils.ButtonData.of("second-" + s, String.format("%02d", s)))
                .toArray(KeyboardMarkupUtils.ButtonData[]::new);

        InlineKeyboardMarkup keyboard = KeyboardMarkupUtils.createLayoutWithCancel(getPrefix(), 6, secondButtons);
        customTelegramClient.editMessage(
                state.getChatId(),
                state.getMessageId(),
                "Select the seconds",
                keyboard
        );
    }

    private void showDateSelection(FlowState state) {
        List<KeyboardMarkupUtils.ButtonData> dateButtons = new ArrayList<>();
        
        // Add "Today" button
        dateButtons.add(KeyboardMarkupUtils.ButtonData.of("date-today", "Today"));
        
        // Add last 7 days
        LocalDate today = LocalDate.now();
        for (int i = 1; i <= 3; i++) {
            LocalDate date = today.minusDays(i);
            dateButtons.add(KeyboardMarkupUtils.ButtonData.of(
                    "date-" + date.toString(),
                    FormatUtils.formatDate(date)
            ));
        }

        InlineKeyboardMarkup keyboard = KeyboardMarkupUtils.createTwoColumnLayoutWithCancel(
                getPrefix(),
                dateButtons.toArray(new KeyboardMarkupUtils.ButtonData[0])
        );
        
        customTelegramClient.editMessage(
            state.getChatId(), 
            state.getMessageId(),
                "Select the date",
            keyboard
        );
    }

    private void showConfirmation(FlowState state) {
        try {
            TelegramUser selectedUser = telegramUserService.find(state.getSelectedUserId())
                    .orElseThrow(() -> new UserNotFoundException(state.getChatId(), new UserInfo(null, "Unknown", "", "")));
            UserInfo userInfo = telegramUserAdapter.adapt(selectedUser);
            GameInfo gameInfo = gameTypeAdapter.adapt(state.getSelectedGame());
            
            ChatInfo chatInfo = new ChatInfo(state.getChatId(), null, true);
            Optional<GameSession> existingSession = gameSessionService.getDaySession(
                    chatInfo, 
                    userInfo, 
                    state.getSelectedGame(), 
                    state.getSelectedDate()
            );
            
            String warningText = existingSession
                    .map(session -> """
                            
                            ⚠️ <b>Warning: Existing Record</b> ⚠️
                            
                            A result already exists for this user on the selected date, with a duration of: <b>%s</b>.
                            Proceeding will <b>replace</b> the existing record.
                            
                            """.formatted(FormatUtils.formatDuration(session.getDuration())))
                    .orElse("");
            
            String confirmationText = String.format("""
                    You are about to register:
                    
                    👤 User: %s
                    🧩 Game: %s
                    📅 Date: %s
                    ⏳ Duration: %s
                    
                    %sDo you want to proceed?
                    """, 
                    FormatUtils.formatUserMention(userInfo), 
                    gameInfo.name(), 
                    FormatUtils.formatDate(state.getSelectedDate()),
                    FormatUtils.formatDuration(state.getDuration()), 
                    warningText
            );
            
            InlineKeyboardMarkup keyboard = KeyboardMarkupUtils.createTwoColumnLayout(
                    getPrefix(),
                    KeyboardMarkupUtils.ButtonData.of("confirm", "✅ Yes"),
                    KeyboardMarkupUtils.ButtonData.of("cancel", "❌ No")
            );
            
            state.setCurrentStep(FlowStep.CONFIRMATION);
            customTelegramClient.editMessage(state.getChatId(), state.getMessageId(), confirmationText, keyboard);
            
        } catch (UserNotFoundException e) {
            LOGGER.error("User not found: {}", state.getSelectedUserId(), e);
            customTelegramClient.editMessage(state.getChatId(), state.getMessageId(), "Failed to load user information.");
            cleanupFlow(state);
        }
    }

    private void handleCallback(Update update) {
        Long chatId = AbilityUtils.getChatId(update);
        Long userId = update.getCallbackQuery().getFrom().getId();
        String flowKey = chatId + ":" + userId;
        String callbackQueryId = update.getCallbackQuery().getId();
        MaybeInaccessibleMessage message = update.getCallbackQuery().getMessage();
        Integer messageId = message.getMessageId();
        
        // Check for legacy confirmation flow (backward compatibility)
        String action = getAction(update);
        if (action.startsWith("confirm-") || action.startsWith("cancel-")) {
            handleLegacyConfirmation(update);
            return;
        }
        
        // Get flow state
        FlowState state = flowStates.getIfPresent(flowKey);
        
        if (state == null) {
            customTelegramClient.answerCallbackQuery(callbackQueryId, "Session expired. Please start over with /override", true);
            return;
        }
        
        // Verify authorization
        if (!isAuthorizedUser(update, state.getInitiatorUserId())) {
            customTelegramClient.answerCallbackQuery(callbackQueryId, "This menu is not for you.", false);
            return;
        }
        
        // Store message ID if not set
        if (state.getMessageId() == null) {
            state.setMessageId(messageId);
        }
        
        // Route based on action
        if (action.startsWith("user-")) {
            handleUserSelection(state, action, callbackQueryId);
        } else if (action.startsWith("game-")) {
            handleGameSelection(state, action, callbackQueryId);
        } else if (action.startsWith("minute-")) {
            handleMinuteSelection(state, action, callbackQueryId);
        } else if (action.startsWith("second-")) {
            handleSecondSelection(state, action, callbackQueryId);
        } else if (action.startsWith("date-")) {
            handleDateSelection(state, action, callbackQueryId);
        } else if (action.equals("confirm")) {
            handleConfirmation(state, callbackQueryId);
        } else if (action.equals("cancel")) {
            handleCancellation(state, callbackQueryId);
        }
    }

    private void handleUserSelection(FlowState state, String action, String callbackQueryId) {
        Long selectedUserId = Long.parseLong(action.substring("user-".length()));
        state.setSelectedUserId(selectedUserId);
        state.setCurrentStep(FlowStep.GAME_SELECTION);
        
        customTelegramClient.answerCallbackQuery(callbackQueryId);
        showGameSelection(state);
    }

    private void handleGameSelection(FlowState state, String action, String callbackQueryId) {
        String gameName = action.substring("game-".length());
        GameType gameType = GameType.valueOf(gameName);
        state.setSelectedGame(gameType);
        
        customTelegramClient.answerCallbackQuery(callbackQueryId);
        showMinuteSelection(state);
    }

    private void handleMinuteSelection(FlowState state, String action, String callbackQueryId) {
        int selectedMinutes = Integer.parseInt(action.substring("minute-".length()));
        state.setSelectedMinutes(selectedMinutes);

        customTelegramClient.answerCallbackQuery(callbackQueryId);
        showSecondSelection(state);
    }

    private void handleSecondSelection(FlowState state, String action, String callbackQueryId) {
        int selectedSeconds = Integer.parseInt(action.substring("second-".length()));
        Duration duration = Duration.ofMinutes(state.getSelectedMinutes()).plusSeconds(selectedSeconds);

        if (duration.isZero()) {
            customTelegramClient.answerCallbackQuery(callbackQueryId, "Duration must be greater than 0 seconds.", true);
            return;
        }

        state.setDuration(duration);
        state.setCurrentStep(FlowStep.DATE_SELECTION);

        customTelegramClient.answerCallbackQuery(callbackQueryId);
        showDateSelection(state);
    }

    private void handleDateSelection(FlowState state, String action, String callbackQueryId) {
        String dateStr = action.substring("date-".length());
        LocalDate selectedDate = dateStr.equals("today") ? LocalDate.now() : LocalDate.parse(dateStr);
        state.setSelectedDate(selectedDate);
        
        customTelegramClient.answerCallbackQuery(callbackQueryId);
        showConfirmation(state);
    }

    private void handleConfirmation(FlowState state, String callbackQueryId) {
        try {
            TelegramUser selectedUser = telegramUserService.find(state.getSelectedUserId())
                    .orElseThrow(() -> new UserNotFoundException(state.getChatId(), new UserInfo(null, "Unknown", "", "")));
            
            ChatInfo chatInfo = new ChatInfo(state.getChatId(), null, true);
            UserInfo userInfo = telegramUserAdapter.adapt(selectedUser);
            GameDuration gameDuration = new GameDuration(state.getSelectedGame(), state.getDuration());
            
            gameSessionService.recordGameSession(chatInfo, userInfo, gameDuration, state.getSelectedDate(), true);
            
            customTelegramClient.answerCallbackQuery(callbackQueryId);
            customTelegramClient.deleteMessage(state.getChatId(), state.getMessageId());
            cleanupFlow(state);
            
        } catch (Exception e) {
            LOGGER.error("Error recording game session", e);
            customTelegramClient.answerCallbackQuery(callbackQueryId, "Failed to record session.", true);
        }
    }

    private void handleCancellation(FlowState state, String callbackQueryId) {
        customTelegramClient.answerCallbackQuery(callbackQueryId);
        customTelegramClient.editMessage(state.getChatId(), state.getMessageId(), "Operation cancelled");
        cleanupFlow(state);
    }

    private void cleanupFlow(FlowState state) {
        String flowKey = state.getChatId() + ":" + state.getInitiatorUserId();
        flowStates.invalidate(flowKey);
    }

    // Legacy text-argument flow methods
    @SneakyThrows
    private void overrideTime(final Message message, final String[] arguments, boolean pastAllowed) {
        GameType gameType = gameNameAdapter.adapt(arguments[1].trim(), message.getChatId());
        Optional<Duration> duration = ParseUtils.parseIsolatedDuration(arguments[2].trim());
        if (duration.isEmpty()) {
            throw new InvalidUserInputException(INVALID_TIME_FORMAT_MESSAGE, message.getChatId());
        }

        GameDuration gameDuration = new GameDuration(gameType, duration.get());
        if (arguments.length == 4 && !pastAllowed) {
            askForConfirmation(message, arguments, gameDuration);
            return;
        }

        ChatInfo chatInfo = chatAdapter.adapt(message.getChat());
        UserInfo userInfo = getMentionedUser(message, arguments);
        LocalDate gameDay = arguments.length == 4 ? parseDate(message, arguments) : LinkedinTimeUtils.todayGameDay();

        gameSessionService.recordGameSession(chatInfo, userInfo, gameDuration, gameDay, true);
    }

    private void askForConfirmation(final Message message, final String[] arguments, final GameDuration gameDuration) throws InvalidUserInputException, UserNotFoundException {
        LocalDate gameDay = parseDate(message, arguments);

        ChatInfo chatInfo = chatAdapter.adapt(message.getChat());
        UserInfo userInfo = getMentionedUser(message, arguments);
        GameInfo gameInfo = gameTypeAdapter.adapt(gameDuration.type());
        messageCache.put(message.getMessageId(), message);
        Optional<GameSession> daySession = gameSessionService.getDaySession(chatInfo, userInfo, gameDuration.type(), gameDay);

        String warningText = daySession
                .map(session -> """
                        
                        ⚠️ <b>Warning: Existing Record</b> ⚠️
                        
                        A result already exists for user %s on the indicated date, with a duration of: <b>%s</b>.
                        Submitting this will <b>replace</b> the existing record.
                        
                        """.formatted(
                        FormatUtils.formatUserMention(userInfo),
                        FormatUtils.formatDuration(session.getDuration())
                ))
                .orElse("");

        String confirmationText = String.format("""
                                                        You are about to register:
                                                        
                                                        👤 User: %s
                                                        🧩 Game: %s
                                                        📅 Date: %s
                                                        ⏳ Duration: %s
                                                        
                                                        %sDo you want to proceed?
                                                        """, FormatUtils.formatUserMention(userInfo), gameInfo.name(), FormatUtils.formatDate(gameDay),
                                                FormatUtils.formatDuration(gameDuration.duration()), warningText);

        InlineKeyboardMarkup replyButtons = KeyboardMarkupUtils.createTwoColumnLayout(getPrefix(), 
                KeyboardMarkupUtils.ButtonData.of("confirm-" + message.getMessageId(), "✅ Yes"),
                KeyboardMarkupUtils.ButtonData.of("cancel-" + message.getMessageId(), "❌ No"));

        customTelegramClient.sendMessage(chatInfo.chatId(), confirmationText, replyButtons);
    }

    private void handleLegacyConfirmation(Update update) {
        String[] actionParts = getAction(update).split("-");
        String action = actionParts[0];
        int messageId = Integer.parseInt(actionParts[1]);
        MaybeInaccessibleMessage callbackMessage = update.getCallbackQuery().getMessage();
        String callbackQueryId = update.getCallbackQuery().getId();
        
        Message message = messageCache.getIfPresent(messageId);
        if (message == null || !message.getFrom().getId().equals(AbilityUtils.getUser(update).getId())) {
            customTelegramClient.answerCallbackQuery(callbackQueryId);
            customTelegramClient.editMessage(callbackMessage.getChatId(), callbackMessage.getMessageId(), "<< expired message >>");
            return;
        }
        
        switch (action) {
            case "confirm":
                String[] parts = message.getText().trim().split("\\s+");
                String[] arguments = Arrays.copyOfRange(parts, 1, parts.length);
                overrideTime(message, arguments, true);
                messageCache.invalidate(messageId);
                customTelegramClient.answerCallbackQuery(callbackQueryId);
                customTelegramClient.deleteMessage(message.getChatId(), callbackMessage.getMessageId());
                break;
            case "cancel":
                messageCache.invalidate(messageId);
                customTelegramClient.answerCallbackQuery(callbackQueryId);
                customTelegramClient.editMessage(callbackMessage.getChatId(), callbackMessage.getMessageId(), "Operation cancelled");
                break;
        }
    }

    private LocalDate parseDate(final Message message, final String[] arguments) throws InvalidUserInputException {
        LocalDate gameDay;
        try {
            gameDay = LocalDate.parse(arguments[3].trim());
        } catch (DateTimeParseException e) {
            throw new InvalidUserInputException("Invalid date format. Use yyyy-MM-dd.", message.getChatId());
        }

        if (gameDay.isAfter(LocalDate.now())) {
            throw new InvalidUserInputException("Date cannot be in the future.", message.getChatId());
        }
        return gameDay;
    }

    private UserInfo getMentionedUser(final Message message, final String[] arguments) throws UserNotFoundException {
        Optional<User> mentionedUser = Optional.ofNullable(message.getEntities())
                                               .flatMap(entities -> entities.stream()
                                                                            .filter(entity -> "text_mention".equals(entity.getType()))
                                                                            .findFirst()
                                                                            .map(MessageEntity::getUser));

        if (mentionedUser.isPresent()) {
            return userAdapter.adapt(mentionedUser.get());
        }
        String username = arguments[0].startsWith("@") ? arguments[0].substring(1) : arguments[0];
        TelegramUser telegramUser = telegramUserService.findByUserName(username)
                                                       .orElseThrow(() -> new UserNotFoundException(message.getChatId(), new UserInfo(null, username, "", "")));
        return telegramUserAdapter.adapt(telegramUser);
    }

    // Flow state management
    @Getter
    @Setter
    private static class FlowState {
        private Long chatId;
        private Long initiatorUserId;
        private Integer messageId;
        private FlowStep currentStep;
        private Long selectedUserId;
        private GameType selectedGame;
        private Integer selectedMinutes;
        private Duration duration;
        private LocalDate selectedDate;
    }

    private enum FlowStep {
        USER_SELECTION,
        GAME_SELECTION,
        MINUTE_SELECTION,
        SECOND_SELECTION,
        DATE_SELECTION,
        CONFIRMATION
    }
}
