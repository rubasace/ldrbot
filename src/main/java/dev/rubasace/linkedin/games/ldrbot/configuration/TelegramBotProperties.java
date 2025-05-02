package dev.rubasace.linkedin.games.ldrbot.configuration;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@Getter
@ConfigurationProperties("dev.rubasace.linkedin.bot")
public class TelegramBotProperties {


    private final String url;
    private final String username;
    private final String token;

    public TelegramBotProperties(@NotBlank final String url, @NotBlank final String username, @NotBlank final String token) {
        this.url = url.replaceAll("/+$", "");
        this.username = username;
        this.token = token;
    }
}
