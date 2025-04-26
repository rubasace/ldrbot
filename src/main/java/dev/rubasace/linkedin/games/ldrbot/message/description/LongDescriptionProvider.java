package dev.rubasace.linkedin.games.ldrbot.message.description;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.description.SetMyDescription;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
class LongDescriptionProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(LongDescriptionProvider.class);
    private static final String BOT_LONG_DESCRIPTION = "When you add LDRBot to a Telegram group, that group becomes its own independent leaderboard and competition space. Each day, members of the group can submit their results for LinkedIn’s puzzles (currently: Queens, Tango, and Zip) by simply uploading a screenshot of their completion screen.";

    private final TelegramClient telegramClient;

    LongDescriptionProvider(final TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    @PostConstruct
    void setDescription() {
        try {
            telegramClient.execute(new SetMyDescription(BOT_LONG_DESCRIPTION, "en"));
        } catch (TelegramApiException e) {
            LOGGER.error("Failed to register long description", e);
        }
    }
}
