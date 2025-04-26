package dev.rubasace.linkedin.games.ldrbot.message.abilities;

import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.message.AbilityImplementation;
import dev.rubasace.linkedin.games.ldrbot.message.ChatAdapter;
import dev.rubasace.linkedin.games.ldrbot.message.GameNameAdapter;
import dev.rubasace.linkedin.games.ldrbot.message.InvalidUserInputException;
import dev.rubasace.linkedin.games.ldrbot.message.UserAdapter;
import dev.rubasace.linkedin.games.ldrbot.session.GameSessionService;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import dev.rubasace.linkedin.games.ldrbot.util.InputSanitizer;
import dev.rubasace.linkedin.games.ldrbot.util.UsageFormatUtils;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.abilitybots.api.objects.Ability;
import org.telegram.telegrambots.abilitybots.api.objects.Locality;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import static org.telegram.telegrambots.abilitybots.api.objects.Privacy.PUBLIC;

@Component
class DeleteAbility implements AbilityImplementation {

    public static final String INVALID_ARGUMENTS_MESSAGE = """
            Please provide a gameInfo name.
            
            Example: <code>/delete queens</code>
            """;
    private final GameSessionService gameSessionService;
    private final ChatAdapter chatAdapter;
    private final UserAdapter userAdapter;
    private final GameNameAdapter gameNameAdapter;

    DeleteAbility(final GameSessionService gameSessionService, final ChatAdapter chatAdapter, final UserAdapter userAdapter, final GameNameAdapter gameNameAdapter) {
        this.gameSessionService = gameSessionService;
        this.chatAdapter = chatAdapter;
        this.userAdapter = userAdapter;
        this.gameNameAdapter = gameNameAdapter;
    }

    @Override
    public Ability getAbility() {
        return Ability.builder()
                      .name("delete")
                      .info(UsageFormatUtils.formatUsage("/delete <gameInfo>", "Remove your submitted time for a gameInfo."))
                      .locality(Locality.GROUP)
                      .privacy(PUBLIC)
                      .action(ctx -> deleteTodayRecord(ctx.update().getMessage(), InputSanitizer.sanitizeArguments(ctx.arguments())))
                      .build();
    }

    @SneakyThrows
    private void deleteTodayRecord(final Message message, final String[] arguments) {
        if (arguments.length != 1) {
            throw new InvalidUserInputException(INVALID_ARGUMENTS_MESSAGE, message.getChatId());
        }

        String gameName = arguments[0];
        ChatInfo chatInfo = chatAdapter.adapt(message.getChat());
        UserInfo userInfo = userAdapter.adapt(message.getFrom());
        GameType gameType = gameNameAdapter.adapt(gameName, message.getChatId());
        gameSessionService.deleteTodaySession(chatInfo, userInfo, gameType);
    }


}
