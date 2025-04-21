package dev.rubasace.linkedin.games_tracker.chat;

import dev.rubasace.linkedin.games_tracker.configuration.TelegramBotProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.abilitybots.api.bot.AbilityBot;
import org.telegram.telegrambots.abilitybots.api.db.MapDBContext;
import org.telegram.telegrambots.abilitybots.api.objects.Ability;
import org.telegram.telegrambots.abilitybots.api.objects.Locality;
import org.telegram.telegrambots.abilitybots.api.objects.Privacy;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Component
public class ChatController extends AbilityBot implements SpringLongPollingBot {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    private final String token;
    private final Executor controllerExecutor = Executors.newVirtualThreadPerTaskExecutor();

    ChatController(final TelegramClient telegramClient,
                   final ChatService chatService,
                   final TelegramBotProperties telegramBotProperties) {
        //TODO investigate if DB has/can be made persistent against real DB
        super(telegramClient, telegramBotProperties.getUsername(), MapDBContext.onlineInstance("/tmp/" + telegramBotProperties.getUsername()));
        this.chatService = chatService;
        this.token = telegramBotProperties.getToken();
    }

    @Override
    public void consume(final List<Update> updates) {
        controllerExecutor.execute(() -> updates.forEach(this::consume));
    }

    @Override
    public void consume(Update update) {
        super.consume(update);
        if (!update.hasMessage()) {
            return;
        }
        if (update.getMessage().isCommand()) {
            return;
        }
        chatService.processMessage(update.getMessage());
    }

    @PostConstruct
    void registerCommands() {
        super.onRegister();
        List<BotCommand> commands = getAbilities().values().stream()
                                                  .filter(ability -> ability.info() != null)
                                                  .map(ability -> new BotCommand(ability.name(), ability.info()))
                                                  .toList();
        try {
            telegramClient.execute(new SetMyCommands(commands));
        } catch (TelegramApiException e) {
            LOGGER.error("Failed to register bot commands", e);
        }
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public long creatorId() {
        //It won't have creator specific commands for now
        return -1;
    }

    //TODO allow to submit and delete/deleteall on private chat, affecting all joined groups
    //TODO move actions to separate classes to control a bit better the implementation (probably move away from main chatService into dedicated components)
    public Ability start() {
        return Ability.builder()
                      .name("start")
                      .info("Initialize the bot for this group and start tracking game results.")
                      .locality(Locality.GROUP)
                      .privacy(Privacy.PUBLIC)
                      .action(ctx -> chatService.setupGroup(ctx.update().getMessage().getChat()))
                      .build();
    }

    public Ability join() {
        return Ability.builder()
                      .name("join")
                      .info("Register yourself as a participant in the group. (This happens automatically when you submit your first message in the group.)")
                      .locality(Locality.GROUP)
                      .privacy(Privacy.PUBLIC)
                      .action(ctx -> chatService.addUserToGroup(ctx.update().getMessage()))
                      .build();
    }

    public Ability delete() {
        return Ability.builder()
                      .name("delete")
                      .info("Delete your game result for today. Usage: /delete <game>")
                      .input(1)
                      .locality(Locality.GROUP)
                      .privacy(Privacy.PUBLIC)
                      .action(ctx -> chatService.deleteTodayRecord(ctx.update().getMessage(), ctx.arguments()))
                      .build();
    }

    public Ability deleteAll() {
        return Ability.builder()
                      .name("deleteall")
                      .info("Delete all your submitted results for today.")
                      .locality(Locality.GROUP)
                      .privacy(Privacy.PUBLIC)
                      .action(ctx -> chatService.deleteTodayRecords(ctx.update().getMessage()))
                      .build();
    }

    public Ability dailyRanking() {
        return Ability.builder()
                      .name("daily")
                      .info("Manually trigger today's group ranking summary.")
                      .locality(Locality.GROUP)
                      .privacy(Privacy.PUBLIC)
                      .action(ctx -> chatService.dailyRanking(ctx.update().getMessage()))
                      .build();
    }

    public Ability games() {
        return Ability.builder()
                      .name("games")
                      .info("Show the list of games currently tracked by this group.")
                      .locality(Locality.GROUP)
                      .privacy(Privacy.PUBLIC)
                      .action(ctx -> chatService.listTrackedGames(ctx.update().getMessage()))
                      .build();
    }

    public Ability override() {
        return Ability.builder()
                      .name("override")
                      .info("Admin-only: Manually override a user's game time (mm:ss) in case the bot fails to detect it. Usage: /override @<username> <game> <duration>")
                      .input(3)
                      .locality(Locality.GROUP)
                      .privacy(Privacy.GROUP_ADMIN)
                      .action(ctx -> chatService.registerSessionManually(ctx.update().getMessage(), ctx.arguments()))
                      .build();
    }
}