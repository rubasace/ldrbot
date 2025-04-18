package dev.rubasace.linkedin.games_tracker.chat;

import dev.rubasace.linkedin.games_tracker.configuration.TelegramBotProperties;
import dev.rubasace.linkedin.games_tracker.session.UnrecognizedGameException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.abilitybots.api.bot.AbilityBot;
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
    private final MessageService messageService;
    private final String token;
    private final Executor controllerExecutor = Executors.newVirtualThreadPerTaskExecutor();

    ChatController(final TelegramClient telegramClient,
                   final ChatService chatService,
                   final MessageService messageService,
                   final TelegramBotProperties telegramBotProperties) {
        super(telegramClient, telegramBotProperties.getUsername());
        this.chatService = chatService;
        this.messageService = messageService;
        this.token = telegramBotProperties.getToken();
    }


    @Override
    public void consume(final List<Update> updates) {
        updates.forEach(update -> controllerExecutor.execute(() -> consume(update)));
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
    public void registerCommands() {
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

    public Ability start() {
        return Ability
                .builder()
                .name("start")
                .info("Makes the bot start tracking the results of the group members, giving daily summaries")
                .locality(Locality.GROUP)
                .privacy(Privacy.PUBLIC)
                .action(context -> chatService.setupGroup(context.update().getMessage().getChat()))
                .build();
    }

    public Ability join() {
        return Ability
                .builder()
                .name("join")
                .info("Register yourself as a participant of the group. Alternatively, users will be registered the moment they submit their first result")
                .locality(Locality.GROUP)
                .privacy(Privacy.PUBLIC)
                .action(context -> chatService.addUserToGroup(context.update().getMessage()))
                .build();
    }

    public Ability delete() {
        return Ability.builder()
                      .name("delete")
                      .info("Delete your existing game submission. Usage: /delete <game>")
                      .input(1)
                      .locality(Locality.ALL)
                      .privacy(Privacy.PUBLIC)
                      .action(ctx -> {
                          if (ctx.arguments() == null || ctx.arguments().length == 0) {
                              messageService.error("You must provide a game name. Example: /delete queens", ctx.chatId());
                              return;
                          }

                          String game = ctx.arguments()[0]; // expecting something like 'QUEENS'
                          try {
                              chatService.deleteTodayRecord(ctx.update().getMessage(), game);
                              messageService.info("Your record for %s has been deleted.".formatted(game), ctx.chatId());
                          } catch (UnrecognizedGameException e) {
                              messageService.error("%s isn't a valid game".formatted(e.getGameName()), ctx.chatId());
                          }
                      })
                      .build();
    }

    public Ability deleteAll() {
        return Ability.builder()
                      .name("deleteall")
                      .info("Delete all your game submissions for today")
                      .locality(Locality.ALL)
                      .privacy(Privacy.PUBLIC)
                      .action(ctx -> {
                          chatService.deleteTodayRecords(ctx.update().getMessage());
                          messageService.info("Your records for today have been deleted.", ctx.chatId());
                      })
                      .build();
    }
}