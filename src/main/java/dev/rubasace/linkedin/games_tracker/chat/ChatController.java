package dev.rubasace.linkedin.games_tracker.chat;

import dev.rubasace.linkedin.games_tracker.configuration.TelegramBotProperties;
import jakarta.annotation.PostConstruct;
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

//TODO improve threading (use virtual threads and allow parallel processing)
@Component
public class ChatController extends AbilityBot implements SpringLongPollingBot {

    private final ChatService chatService;
    private final String token;

    ChatController(final TelegramClient telegramClient, final ChatService chatService, final TelegramBotProperties telegramBotProperties) {
        super(telegramClient, telegramBotProperties.getUsername());
        this.chatService = chatService;
        this.token = telegramBotProperties.getToken();
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
        if (update.getMessage().hasPhoto()) {
            chatService.processPhoto(update.getMessage().getPhoto(), update.getMessage().getFrom().getUserName(), update.getMessage().getChatId());
        } else if (update.getMessage().hasDocument() && update.getMessage().getDocument().getThumbnail() != null) {
            chatService.processPhoto(List.of(update.getMessage().getDocument().getThumbnail()), update.getMessage().getFrom().getUserName(), update.getMessage().getChatId());
        }

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
            System.err.println("❌ Failed to register bot commands");
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
                .action(context -> chatService.setupGroup(context.update().getMessage()))
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
}