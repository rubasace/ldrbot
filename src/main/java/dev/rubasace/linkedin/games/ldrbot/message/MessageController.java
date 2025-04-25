package dev.rubasace.linkedin.games.ldrbot.message;

import dev.rubasace.linkedin.games.ldrbot.configuration.TelegramBotProperties;
import dev.rubasace.linkedin.games.ldrbot.exception.HandleBotExceptions;
import dev.rubasace.linkedin.games.ldrbot.util.UsageFormatUtils;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
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

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

import static org.telegram.telegrambots.abilitybots.api.objects.Locality.ALL;
import static org.telegram.telegrambots.abilitybots.api.objects.Privacy.PUBLIC;

@HandleBotExceptions
@Component
public class MessageController extends AbilityBot implements SpringLongPollingBot {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageController.class);
    public static final int MAX_CONSUME_CONCURRENCY = 25;

    private final MessageService messageService;
    private final String token;
    private final ExecutorService controllerExecutor;
    private final Semaphore semaphore;

    MessageController(final TelegramClient telegramClient,
                      final MessageService messageService,
                      final TelegramBotProperties telegramBotProperties) {
        super(telegramClient, telegramBotProperties.getUsername(), MapDBContext.onlineInstance("/tmp/" + telegramBotProperties.getUsername()));
        this.messageService = messageService;
        this.token = telegramBotProperties.getToken();
        this.controllerExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.semaphore = new Semaphore(MAX_CONSUME_CONCURRENCY);
    }

    @Override
    public void consume(final List<Update> updates) {
        controllerExecutor.execute(() -> updates.forEach(update -> {
            try {
                semaphore.acquire();
                consume(update);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.error("Update consumption interrupted", e);
            } finally {
                semaphore.release();
            }
        }));
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage() || update.getMessage().getFrom().getIsBot()) {
            return;
        }
        if (update.getMessage().getChat().isGroupChat()) {
            messageService.registerOrUpdateGroup(update.getMessage());
        }
        super.consume(update);
        messageService.processMessage(update.getMessage());
    }

    @Override
    public Map<String, Ability> getAbilities() {
        return super.getAbilities().values().stream()
                    .filter(ability -> ability.info() != null)
                    .collect(Collectors.toMap(Ability::name, ability -> ability));
    }

    @PostConstruct
    void registerCommands() {
        super.onRegister();
        unregisterUnknownAbilities();
        List<BotCommand> commands = getAbilities().values().stream()
                                                  .map(ability -> new BotCommand(ability.name(), ability.info()))
                                                  .toList();
        messageService.registerCommands(commands);
        try {
            telegramClient.execute(new SetMyCommands(commands));
        } catch (TelegramApiException e) {
            LOGGER.error("Failed to register bot commands", e);
        }
    }

    private void unregisterUnknownAbilities() {
        Field abilities = ReflectionUtils.findField(this.getClass(), "abilities");
        abilities.setAccessible(true);
        ReflectionUtils.setField(abilities, this, getAbilities());
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

    //TODO add metrics
    //TODO allow to submit and delete/deleteall on private chat, affecting all joined groups
    //TODO move actions to separate classes to control a bit better the implementation (probably move away from main chatService into dedicated components)
    public Ability start() {
        return Ability.builder()
                      .name("start")
                      .info("Start interacting with LDRBot. Required for private messages.")
                      .locality(ALL)
                      .privacy(PUBLIC)
                      .action(ctx -> messageService.start(ctx.update().getMessage()))
                      .build();
    }

    public Ability delete() {
        return Ability.builder()
                      .name("delete")
                      .info(UsageFormatUtils.formatUsage("/delete <game>", "Remove your submitted time for a game."))
                      .input(1)
                      .locality(Locality.GROUP)
                      .privacy(PUBLIC)
                      .action(ctx -> messageService.deleteTodayRecord(ctx.update().getMessage(), ctx.arguments()))
                      .build();
    }

    public Ability deleteAll() {
        return Ability.builder()
                      .name("deleteall")
                      .info("Remove all your submitted results for today.")
                      .locality(Locality.GROUP)
                      .privacy(PUBLIC)
                      .action(ctx -> messageService.deleteTodayRecords(ctx.update().getMessage()))
                      .build();
    }

    public Ability dailyRanking() {
        return Ability.builder()
                      .name("ranking")
                      .info("Show today’s group leaderboard.")
                      .locality(Locality.GROUP)
                      .privacy(PUBLIC)
                      .action(ctx -> messageService.dailyRanking(ctx.update().getMessage()))
                      .build();
    }

    public Ability games() {
        return Ability.builder()
                      .name("games")
                      .info("List the games tracked by this group.")
                      .locality(Locality.GROUP)
                      .privacy(PUBLIC)
                      .action(ctx -> messageService.listTrackedGames(ctx.update().getMessage()))
                      .build();
    }

    public Ability override() {
        return Ability.builder()
                      .name("override")
                      .info(UsageFormatUtils.formatUsage("/override @<user> <game> <mm:ss>", "Manually set a user’s time (admin-only)."))
                      .input(3)
                      .locality(Locality.GROUP)
                      .privacy(Privacy.GROUP_ADMIN)
                      .action(ctx -> messageService.registerSessionManually(ctx.update().getMessage(), ctx.arguments()))
                      .build();
    }

    public Ability help() {
        return Ability.builder()
                      .name("help")
                      .info("Show available commands and how to use the bot.")
                      .locality(ALL)
                      .privacy(PUBLIC)
                      .action(ctx -> messageService.help(ctx.update().getMessage()))
                      .build();
    }


}