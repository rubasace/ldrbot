package dev.rubasace.linkedin.games.ldrbot.message.abilities;

import dev.rubasace.linkedin.games.ldrbot.exception.HandleBotExceptions;
import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.message.AbilityImplementation;
import dev.rubasace.linkedin.games.ldrbot.message.ChatAdapter;
import dev.rubasace.linkedin.games.ldrbot.message.UserAdapter;
import dev.rubasace.linkedin.games.ldrbot.session.GameSessionService;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import dev.rubasace.linkedin.games.ldrbot.util.LinkedinTimeUtils;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.abilitybots.api.objects.Ability;
import org.telegram.telegrambots.abilitybots.api.objects.Locality;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import static org.telegram.telegrambots.abilitybots.api.objects.Privacy.PUBLIC;

@HandleBotExceptions
@Component
class DeleteAllAbility implements AbilityImplementation {

    private final GameSessionService gameSessionService;
    private final ChatAdapter chatAdapter;
    private final UserAdapter userAdapter;

    DeleteAllAbility(final GameSessionService gameSessionService, final ChatAdapter chatAdapter, final UserAdapter userAdapter) {
        this.gameSessionService = gameSessionService;
        this.chatAdapter = chatAdapter;
        this.userAdapter = userAdapter;
    }

    @Override
    public Ability getAbility() {
        return Ability.builder()
                      .name("deleteall")
                      .info("Remove all your submitted results for today.")
                      .locality(Locality.GROUP)
                      .privacy(PUBLIC)
                      .action(ctx -> deleteTodayRecords(ctx.update().getMessage()))
                      .build();
    }

    @SneakyThrows
    private void deleteTodayRecords(final Message message) {
        ChatInfo chatInfo = chatAdapter.adapt(message.getChat());
        UserInfo userInfo = userAdapter.adapt(message.getFrom());
        gameSessionService.deleteDaySessions(chatInfo, userInfo, LinkedinTimeUtils.todayGameDay());
    }

}
