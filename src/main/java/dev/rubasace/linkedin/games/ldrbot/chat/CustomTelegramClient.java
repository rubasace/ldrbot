package dev.rubasace.linkedin.games.ldrbot.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Service
class CustomTelegramClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomTelegramClient.class);

    private final TelegramClient telegramClient;

    CustomTelegramClient(final TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    void html(final String text, final Long chatId) {
        sendMessage(text, chatId);
    }

    void info(final String text, final Long chatId) {
        sendMessage(text, chatId);
    }

    void success(final String text, final Long chatId) {
        sendMessage("✅ " + text, chatId);
    }

    void error(final String text, final Long chatId) {
        sendMessage("❌ " + text, chatId);
    }

    void reminder(final String text, final Long chatId) {
        String formatted = """
                    ⏰ <b>Don't forget!</b> ⏰
                
                    %s
                """.formatted(text);
        sendMessage(formatted, chatId);
    }

    private void sendMessage(final String text, final Long chatId) {
        SendMessage message = SendMessage.builder()
                                         .chatId(chatId)
                                         .text(text)
                                         .parseMode(ParseMode.HTML)
                                         .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            LOGGER.error("Error sending message", e);
        }
    }

}
