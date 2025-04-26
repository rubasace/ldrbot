package dev.rubasace.linkedin.games.ldrbot.message.abilities;

import dev.rubasace.linkedin.games.ldrbot.exception.HandleBotExceptions;
import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.message.AbilityImplementation;
import dev.rubasace.linkedin.games.ldrbot.message.ChatAdapter;
import dev.rubasace.linkedin.games.ldrbot.message.InvalidUserInputException;
import dev.rubasace.linkedin.games.ldrbot.message.UserAdapter;
import dev.rubasace.linkedin.games.ldrbot.session.GameNameNotFoundException;
import dev.rubasace.linkedin.games.ldrbot.session.GameSessionService;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import dev.rubasace.linkedin.games.ldrbot.util.UsageFormatUtils;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.abilitybots.api.objects.Ability;
import org.telegram.telegrambots.abilitybots.api.objects.Locality;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import static org.telegram.telegrambots.abilitybots.api.objects.Privacy.PUBLIC;

@HandleBotExceptions
@Component
class DeleteAbility implements AbilityImplementation {

    private final GameSessionService gameSessionService;
    private final ChatAdapter chatAdapter;
    private final UserAdapter userAdapter;

    DeleteAbility(final GameSessionService gameSessionService, final ChatAdapter chatAdapter, final UserAdapter userAdapter) {
        this.gameSessionService = gameSessionService;
        this.chatAdapter = chatAdapter;
        this.userAdapter = userAdapter;
    }

    @Override
    public Ability getAbility() {
        return Ability.builder()
                      .name("delete")
                      .info(UsageFormatUtils.formatUsage("/delete <game>", "Remove your submitted time for a game."))
                      .input(1)
                      .locality(Locality.GROUP)
                      .privacy(PUBLIC)
                      .action(ctx -> deleteTodayRecord(ctx.update().getMessage(), ctx.arguments()))
                      .build();
    }

    @SneakyThrows
    private void deleteTodayRecord(final Message message, final String[] arguments) {
        //TODO think if controlling actions arguments with input() or via explicit arguments check like here. Probably here so we have more control over everything
        if (arguments == null || arguments.length == 0) {
            throw new InvalidUserInputException("Please provide a game name. Example: /delete queens", message.getChatId());
        }

        String gameName = arguments[0];
        ChatInfo chatInfo = chatAdapter.adapt(message.getChat());
        UserInfo userInfo = userAdapter.adapt(message.getFrom());
        GameType gameType = getGameType(gameName, message.getChatId());
        gameSessionService.deleteTodaySession(chatInfo, userInfo, gameType);
    }

    //TODO unify in convenience class (or enum itself)
    private GameType getGameType(final String gameName, final Long chatId) throws GameNameNotFoundException {
        GameType gameType;
        try {
            gameType = GameType.valueOf(gameName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new GameNameNotFoundException(chatId, gameName);
        }
        return gameType;
    }

}
