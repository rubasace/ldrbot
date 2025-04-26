package dev.rubasace.linkedin.games.ldrbot.message.description;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.description.SetMyShortDescription;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
class ShortDescriptionProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShortDescriptionProvider.class);
    private static final String BOT_SHORT_DESCRIPTION = "Tracks and ranks LinkedIn puzzle scores via Telegram. Join the game!";

    private final TelegramClient telegramClient;

    ShortDescriptionProvider(final TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    @PostConstruct
    void setDescription() {
        try {
            telegramClient.execute(new SetMyShortDescription(BOT_SHORT_DESCRIPTION, "en"));
        } catch (TelegramApiException e) {
            LOGGER.error("Failed to register short description", e);
        }
    }
}
