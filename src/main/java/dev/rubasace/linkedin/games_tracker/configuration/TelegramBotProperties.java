package dev.rubasace.linkedin.games_tracker.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("dev.rubasace.linkedin.bot")
public class TelegramBotProperties {

    private final String username;
    private final String token;
}
