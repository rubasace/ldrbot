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
                      .input(3)
                      .locality(Locality.GROUP)
                      .privacy(Privacy.GROUP_ADMIN)
                      .action(ctx -> overrideTime(ctx.update().getMessage(), ctx.arguments()))
                      .build();
    }

    @SneakyThrows
    private void overrideTime(final Message message, final String[] arguments) {
        String username = arguments[0].startsWith("@") ? arguments[0].substring(1) : arguments[0];
        GameType gameType = gameNameAdapter.adapt(arguments[1], message.getChatId());
        Optional<Duration> duration = ParseUtils.parseDuration(arguments[2]);
        if (duration.isEmpty()) {
            throw new InvalidUserInputException("Invalid time format. Use `mm:ss`, e.g. `1:30` or `12:05`", message.getChatId());
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
