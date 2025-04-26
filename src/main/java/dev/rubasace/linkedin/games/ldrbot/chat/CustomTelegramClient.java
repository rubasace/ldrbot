package dev.rubasace.linkedin.games.ldrbot.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Service
public class CustomTelegramClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomTelegramClient.class);

    private final TelegramClient telegramClient;
    private final MessageEscapeHelper messageEscapeHelper;

    CustomTelegramClient(final TelegramClient telegramClient, final MessageEscapeHelper messageEscapeHelper) {
        this.telegramClient = telegramClient;
        this.messageEscapeHelper = messageEscapeHelper;
    }

    public void message(final String text, final Long chatId) {
        sendMessage(text, chatId);
    }

    public void successMessage(final String text, final Long chatId) {
        sendMessage("✅ " + text, chatId);
    }

    public void errorMessage(final String text, final Long chatId) {
        sendMessage("❌ " + text, chatId);
    }

    private void sendMessage(final String text, final Long chatId) {
        SendMessage message = SendMessage.builder()
                                         .chatId(chatId)
                                         .text(messageEscapeHelper.escapeMessage(text))
                                         .parseMode(ParseMode.HTML)
                                         .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            LOGGER.error("Error sending message", e);
        }
    }


}
