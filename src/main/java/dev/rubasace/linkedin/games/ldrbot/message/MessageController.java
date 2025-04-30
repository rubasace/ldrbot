package dev.rubasace.linkedin.games.ldrbot.message;

import dev.rubasace.linkedin.games.ldrbot.chat.UserFeedbackException;
import dev.rubasace.linkedin.games.ldrbot.configuration.TelegramBotProperties;
import dev.rubasace.linkedin.games.ldrbot.group.GroupNotFoundException;
import dev.rubasace.linkedin.games.ldrbot.image.GameDurationExtractionException;
import dev.rubasace.linkedin.games.ldrbot.session.SessionAlreadyRegisteredException;
import dev.rubasace.linkedin.games.ldrbot.util.BackpressureExecutors;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.abilitybots.api.bot.AbilityBot;
import org.telegram.telegrambots.abilitybots.api.db.MapDBContext;
import org.telegram.telegrambots.abilitybots.api.toggle.BareboneToggle;
import org.telegram.telegrambots.abilitybots.api.util.AbilityExtension;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;
import java.util.concurrent.ExecutorService;

@Component
public class MessageController extends AbilityBot implements SpringLongPollingBot {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageController.class);
    public static final int MAX_CONSUME_CONCURRENCY = 25;

    private final MessageService messageService;
    private final ExceptionHandler exceptionHandler;
    private final String token;
    private final ExecutorService controllerExecutor;

    MessageController(final TelegramClient telegramClient,
                      final MessageService messageService,
                      final ExceptionHandler exceptionHandler,
                      final List<AbilityExtension> abilityExtensions,
                      final TelegramBotProperties telegramBotProperties) {
        super(telegramClient, telegramBotProperties.getUsername(), MapDBContext.onlineInstance("/tmp/" + telegramBotProperties.getUsername()), new BareboneToggle());
        this.messageService = messageService;
        this.exceptionHandler = exceptionHandler;
        this.token = telegramBotProperties.getToken();
        this.controllerExecutor = BackpressureExecutors.newBackPressureVirtualThreadPerTaskExecutor("message-controller", MAX_CONSUME_CONCURRENCY);
        this.addExtensions(abilityExtensions);
    }

    @Override
    public void consume(final List<Update> updates) {
        controllerExecutor.execute(() -> updates.forEach(this::consume));
    }

    @Override
    public void consume(Update update) {
        try {
            doConsume(update);
        } catch (Exception e) {
            if (e instanceof UserFeedbackException) {
                exceptionHandler.notifyUserFeedbackException((UserFeedbackException) e);
            } else {
                LOGGER.error("An unexpected error occurred", e);
            }
        }
    }

    private void doConsume(final Update update) throws GameDurationExtractionException, SessionAlreadyRegisteredException, UnknownCommandException, GroupNotFoundException {
        if (update.hasMessage() && update.getMessage().getChat().isGroupChat()) {
            messageService.registerOrUpdateGroup(update.getMessage());
        }
        super.consume(update);
        if (!update.hasMessage() || update.getMessage().getFrom().getIsBot()) {
            return;
        }
        messageService.processMessage(update.getMessage());
    }

    @PostConstruct
    void registerCommands() {
        super.onRegister();
        List<BotCommand> commands = getAbilities().values().stream().map(ability -> new BotCommand(ability.name(), ability.info())).toList();
        messageService.registerCommands(commands);
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

}