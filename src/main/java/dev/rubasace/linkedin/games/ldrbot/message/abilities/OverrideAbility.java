package dev.rubasace.linkedin.games.ldrbot.message.abilities;

import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.message.AbilityImplementation;
import dev.rubasace.linkedin.games.ldrbot.message.ChatAdapter;
import dev.rubasace.linkedin.games.ldrbot.message.GameNameAdapter;
import dev.rubasace.linkedin.games.ldrbot.message.InvalidUserInputException;
import dev.rubasace.linkedin.games.ldrbot.message.UserAdapter;
import dev.rubasace.linkedin.games.ldrbot.session.GameDuration;
import dev.rubasace.linkedin.games.ldrbot.session.GameSessionService;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUser;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUserAdapter;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUserService;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import dev.rubasace.linkedin.games.ldrbot.user.UserNotFoundException;
import dev.rubasace.linkedin.games.ldrbot.util.InputSanitizer;
import dev.rubasace.linkedin.games.ldrbot.util.ParseUtils;
import dev.rubasace.linkedin.games.ldrbot.util.UsageFormatUtils;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.abilitybots.api.objects.Ability;
import org.telegram.telegrambots.abilitybots.api.objects.Locality;
import org.telegram.telegrambots.abilitybots.api.objects.Privacy;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.time.Duration;
import java.util.Optional;

@Component
class OverrideAbility implements AbilityImplementation {

    public static final String INVALID_ARGUMENT_MESSAGE_TEMPLATE = """
            Please provide a user mention, a game name and a time in the format <code>mm:ss</code>.
            
            Example: <code>/override @%s queens 1:30</code>
            """;
    public static final String INVALID_TIME_FORMAT_MESSAGE = """
            Invalid time format.
            
            Use <code>mm:ss</code>, e.g. <code>1:30</code> or <code>12:05</code>
            """;


    private final TelegramUserService telegramUserService;
    private final GameSessionService gameSessionService;
    private final ChatAdapter chatAdapter;
    private final UserAdapter userAdapter;
    private final TelegramUserAdapter telegramUserAdapter;
    private final GameNameAdapter gameNameAdapter;

    OverrideAbility(final TelegramUserService telegramUserService, final GameSessionService gameSessionService, final ChatAdapter chatAdapter, final UserAdapter userAdapter, final TelegramUserAdapter telegramUserAdapter, final GameNameAdapter gameNameAdapter) {
        this.telegramUserService = telegramUserService;
        this.gameSessionService = gameSessionService;
        this.chatAdapter = chatAdapter;
        this.userAdapter = userAdapter;
        this.telegramUserAdapter = telegramUserAdapter;
        this.gameNameAdapter = gameNameAdapter;
    }


    @Override
    public Ability getAbility() {
        return Ability.builder()
                      .name("override")
                      .info(UsageFormatUtils.formatUsage("/override @<user> <game> <mm:ss>", "Manually set a user’s time (admin-only)."))
                      .locality(Locality.GROUP)
                      .privacy(Privacy.GROUP_ADMIN)
                      .action(ctx -> overrideTime(ctx.update().getMessage(), InputSanitizer.sanitizeArguments(ctx.arguments())))
                      .build();
    }

    @SneakyThrows
    private void overrideTime(final Message message, final String[] arguments) {
        if (arguments.length != 3) {
            throw new InvalidUserInputException(INVALID_ARGUMENT_MESSAGE_TEMPLATE.formatted(message.getFrom().getUserName()), message.getChatId());
        }
        String username = arguments[0].startsWith("@") ? arguments[0].substring(1) : arguments[0];
        GameType gameType = gameNameAdapter.adapt(arguments[1], message.getChatId());
        Optional<Duration> duration = ParseUtils.parseDuration(arguments[2]);
        if (duration.isEmpty()) {
            throw new InvalidUserInputException(INVALID_TIME_FORMAT_MESSAGE, message.getChatId());
        }
        ChatInfo chatInfo = chatAdapter.adapt(message.getChat());
        Optional<User> mentionedUser = getMentionedUser(message);
        if (mentionedUser.isPresent()) {
            UserInfo userInfo = userAdapter.adapt(mentionedUser.get());
            gameSessionService.recordGameSession(chatInfo, userInfo, new GameDuration(gameType, duration.get()));
        } else {
            TelegramUser telegramUser = telegramUserService.findByUserName(username)
                                                           .orElseThrow(() -> new UserNotFoundException(message.getChatId(), new UserInfo(null, username, "", "")));
            UserInfo userInfo = telegramUserAdapter.adapt(telegramUser);
            gameSessionService.recordGameSession(chatInfo, userInfo, new GameDuration(gameType, duration.get()));
        }
    }

    private Optional<User> getMentionedUser(final Message message) {
        return Optional.ofNullable(message.getEntities())
                       .flatMap(entities -> entities.stream()
                                                    .filter(entity -> "text_mention".equals(entity.getType()))
                                                    .findFirst()
                                                    .map(MessageEntity::getUser));

    }
}
