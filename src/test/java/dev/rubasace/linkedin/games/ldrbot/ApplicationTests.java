package dev.rubasace.linkedin.games.ldrbot;

import dev.rubasace.linkedin.games.ldrbot.chat.CustomTelegramClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.telegram.telegrambots.longpolling.starter.TelegramBotInitializer;

@SpringBootTest
class ApplicationTests {

    @MockitoBean
    private TelegramBotInitializer telegramBotInitializer;

    @MockitoBean
    private CustomTelegramClient customTelegramClient;

    @Test
    void contextLoads() {
    }

}
