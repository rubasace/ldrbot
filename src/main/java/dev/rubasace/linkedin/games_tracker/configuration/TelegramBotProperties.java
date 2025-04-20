package dev.rubasace.linkedin.games_tracker.configuration;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@Getter
@ConfigurationProperties("dev.rubasace.linkedin.bot")
public class TelegramBotProperties {

    @NotBlank
    private final String username;
    @NotBlank
    private final String token;

    public TelegramBotProperties(final String username, final String token) {
        this.username = username;
        this.token = token;
    }
}
