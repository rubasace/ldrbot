package dev.rubasace.linkedin.games.ldrbot.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
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

    public void sendOrEditMessage(Long chatId, String text, InlineKeyboardMarkup inlineKeyboardMarkup, Integer messageId) {
        if (messageId != null) {
            editMessage(chatId, messageId, text, inlineKeyboardMarkup);
        } else {
            sendMessage(chatId, text, inlineKeyboardMarkup);
        }
    }

    public void sendMessage(Long chatId, String text, InlineKeyboardMarkup inlineKeyboardMarkup) {
        SendMessage sendMessage = SendMessage.builder()
                                             .chatId(chatId)
                                             .text(text)
                                             .replyMarkup(inlineKeyboardMarkup)
                                             .build();

        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void editMessage(Long chatId, Integer messageId, String text, InlineKeyboardMarkup inlineKeyboardMarkup) {
        EditMessageText editMessage = EditMessageText.builder()
                                                     .chatId(chatId.toString())
                                                     .messageId(messageId)
                                                     .text(text)
                                                     .replyMarkup(inlineKeyboardMarkup)
                                                     .build();

        try {
            telegramClient.execute(editMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteMessage(Long chatId, Integer messageId) {
        try {
            telegramClient.execute(
                    org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage.builder()
                                                                                             .chatId(chatId)
                                                                                             .messageId(messageId)
                                                                                             .build()
            );
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
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
