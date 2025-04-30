package dev.rubasace.linkedin.games.ldrbot.message.abilities;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.rubasace.linkedin.games.ldrbot.chat.CustomTelegramClient;
import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.message.ChatAdapter;
import dev.rubasace.linkedin.games.ldrbot.message.GameNameAdapter;
import dev.rubasace.linkedin.games.ldrbot.message.InvalidUserInputException;
import dev.rubasace.linkedin.games.ldrbot.message.UserAdapter;
import dev.rubasace.linkedin.games.ldrbot.message.config.BaseMessageReplier;
import dev.rubasace.linkedin.games.ldrbot.session.GameDuration;
import dev.rubasace.linkedin.games.ldrbot.session.GameInfo;
import dev.rubasace.linkedin.games.ldrbot.session.GameSession;
import dev.rubasace.linkedin.games.ldrbot.session.GameSessionService;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.session.GameTypeAdapter;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUser;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUserAdapter;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUserService;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import dev.rubasace.linkedin.games.ldrbot.user.UserNotFoundException;
import dev.rubasace.linkedin.games.ldrbot.util.FormatUtils;
import dev.rubasace.linkedin.games.ldrbot.util.InputSanitizer;
import dev.rubasace.linkedin.games.ldrbot.util.KeyboardMarkupUtils;
import dev.rubasace.linkedin.games.ldrbot.util.LinkedinTimeUtils;
import dev.rubasace.linkedin.games.ldrbot.util.ParseUtils;
import dev.rubasace.linkedin.games.ldrbot.util.UsageFormatUtils;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
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
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class OverrideAbility extends BaseMessageReplier implements AbilityExtension {

    public static final String INVALID_ARGUMENT_MESSAGE_TEMPLATE = """
            Invalid input. Please provide a user mention, a game name, and a time in the format <code>mm:ss</code>.
            
            Example usage:
            <code>/override @%s queens 1:30</code> — to override today's record
            <code>/override @%s queens 1:30 2024-09-29</code> — to override a record for a specific past date
            """;
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
    private final Cache<Integer, Message> messageCache;

    OverrideAbility(final TelegramUserService telegramUserService, final GameSessionService gameSessionService, final CustomTelegramClient customTelegramClient, final ChatAdapter chatAdapter, final UserAdapter userAdapter, final TelegramUserAdapter telegramUserAdapter, final GameNameAdapter gameNameAdapter, final GameTypeAdapter gameTypeAdapter) {
        super("override");
        this.telegramUserService = telegramUserService;
        this.gameSessionService = gameSessionService;
        this.customTelegramClient = customTelegramClient;
        this.chatAdapter = chatAdapter;
        this.userAdapter = userAdapter;
        this.telegramUserAdapter = telegramUserAdapter;
        this.gameNameAdapter = gameNameAdapter;
        this.gameTypeAdapter = gameTypeAdapter;
        this.messageCache = Caffeine.newBuilder()
                                    .expireAfterWrite(10, TimeUnit.MINUTES)
                                    .maximumSize(10_000)
                                    .build();
    }


    public Ability override() {
        return Ability.builder()
                      .name("override")
                      .info(UsageFormatUtils.formatUsage(
                              "/override @<user> <game> <mm:ss> [<yyyy-MM-dd>]",
                              "Manually set or update a user’s result (admin-only). Use the optional date to record past games."
                      ))
                      .locality(Locality.GROUP)
                      .privacy(Privacy.GROUP_ADMIN)
                      .reply(this::handleOverrideCallback, this::shouldHandleReply)
                      .action(ctx -> overrideTime(ctx.update().getMessage(), InputSanitizer.sanitizeArguments(ctx.arguments()), false))
                      .build();
    }

    @SneakyThrows
    private void overrideTime(final Message message, final String[] arguments, boolean pastAllowed) {
        if (arguments.length < 3 || arguments.length > 4) {
            throw new InvalidUserInputException(INVALID_ARGUMENT_MESSAGE_TEMPLATE.formatted(message.getFrom().getUserName(), message.getFrom().getUserName()), message.getChatId());
        }
        GameType gameType = gameNameAdapter.adapt(arguments[1].trim(), message.getChatId());
        Optional<Duration> duration = ParseUtils.parseDuration(arguments[2].trim());
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

        InlineKeyboardMarkup replyButtons = KeyboardMarkupUtils.createTwoColumnLayout(getPrefix(), KeyboardMarkupUtils.ButtonData.of("confirm-" + message.getMessageId(), "✅ Yes"),
                                                                                      KeyboardMarkupUtils.ButtonData.of("cancel-" + message.getMessageId(), "❌ No"));

        customTelegramClient.sendMessage(chatInfo.chatId(), confirmationText, replyButtons);

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


    private void handleOverrideCallback(BaseAbilityBot baseAbilityBot, Update update) {
        String[] actionParts = getAction(update).split("-");
        String action = actionParts[0];
        int messageId = Integer.parseInt(actionParts[1]);
        MaybeInaccessibleMessage callbackMessage = update.getCallbackQuery().getMessage();
        Message message = messageCache.getIfPresent(messageId);
        if (message == null || !message.getFrom().getId().equals(AbilityUtils.getUser(update).getId())) {
            customTelegramClient.editMessage(callbackMessage.getChatId(), callbackMessage.getMessageId(), "<< expired message >>");
            return;
        }
        switch (action) {
            case "confirm":
                String[] parts = message.getText().trim().split("\\s+");
                String[] arguments = Arrays.copyOfRange(parts, 1, parts.length);
                overrideTime(message, arguments, true);
                messageCache.invalidate(messageId);
                customTelegramClient.deleteMessage(message.getChatId(), callbackMessage.getMessageId());
                break;
            case "cancel":
                messageCache.invalidate(messageId);
                customTelegramClient.editMessage(callbackMessage.getChatId(), callbackMessage.getMessageId(), "Operation cancelled");
                break;
        }
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
}
