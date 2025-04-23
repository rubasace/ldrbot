package dev.rubasace.linkedin.games.ldrbot.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration
public class TelegramConfiguration {

    @Bean
    TelegramClient telegramClient(final TelegramBotProperties telegramBotProperties) {
        return new OkHttpTelegramClient(telegramBotProperties.getToken());
    }
}
