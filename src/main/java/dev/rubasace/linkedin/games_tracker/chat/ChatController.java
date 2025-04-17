package dev.rubasace.linkedin.games_tracker.chat;

import dev.rubasace.linkedin.games_tracker.configuration.TelegramBotProperties;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Component
class ChatController implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final ChatService chatService;
    private final String token;

    ChatController(final ChatService chatService, final TelegramBotProperties telegramBotProperties) {
        this.chatService = chatService;
        this.token = telegramBotProperties.getToken();
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
    public void consume(Update update) {
        if (!update.hasMessage()) {
            return;
        }
        if (update.getMessage().hasPhoto()) {
            chatService.processPhoto(update.getMessage().getPhoto(), update.getMessage().getFrom().getUserName(), update.getMessage().getChatId());
        } else if (update.getMessage().hasDocument() && update.getMessage().getDocument().getThumbnail() != null) {
            chatService.processPhoto(List.of(update.getMessage().getDocument().getThumbnail()), update.getMessage().getFrom().getUserName(), update.getMessage().getChatId());

        }
    }
}