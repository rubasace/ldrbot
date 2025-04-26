package dev.rubasace.linkedin.games.ldrbot.message;

import dev.rubasace.linkedin.games.ldrbot.chat.NotificationService;
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
import org.springframework.util.ReflectionUtils;
import org.telegram.telegrambots.abilitybots.api.bot.AbilityBot;
import org.telegram.telegrambots.abilitybots.api.db.MapDBContext;
import org.telegram.telegrambots.abilitybots.api.objects.Ability;
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
import java.util.stream.Collectors;

//TODO add /about command
//TODO add metrics
//TODO allow to submit and delete/deleteall on private chat, affecting all joined groups
@Component
public class MessageController extends AbilityBot implements SpringLongPollingBot {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageController.class);
    public static final int MAX_CONSUME_CONCURRENCY = 25;

    private final MessageService messageService;
    private final NotificationService notificationService;
    private final List<AbilityImplementation> abilityImplementations;
    private final String token;
    private final ExecutorService controllerExecutor;

    MessageController(final TelegramClient telegramClient, final MessageService messageService, final NotificationService notificationService, final List<AbilityImplementation> abilityImplementations, final TelegramBotProperties telegramBotProperties) {
        super(telegramClient, telegramBotProperties.getUsername(), MapDBContext.onlineInstance("/tmp/" + telegramBotProperties.getUsername()));
        this.messageService = messageService;
        this.notificationService = notificationService;
        this.abilityImplementations = abilityImplementations;
        this.token = telegramBotProperties.getToken();
        this.controllerExecutor = BackpressureExecutors.newBackPressureVirtualThreadPerTaskExecutor("message-controller", MAX_CONSUME_CONCURRENCY);
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
                notificationService.notifyUserFeedbackException((UserFeedbackException) e);
            } else {
                LOGGER.error("An unexpected error occurred", e);
            }
        }
    }

    private void doConsume(final Update update) throws GameDurationExtractionException, SessionAlreadyRegisteredException, UnknownCommandException, GroupNotFoundException {
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
        return abilityImplementations.stream().map(AbilityImplementation::getAbility).collect(Collectors.toMap(Ability::name, ability -> ability));
    }

    @PostConstruct
    void registerCommands() {
        super.onRegister();
        unregisterUnknownAbilities();
        List<BotCommand> commands = getAbilities().values().stream().map(ability -> new BotCommand(ability.name(), ability.info())).toList();
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

}