package dev.rubasace.linkedin.games.ldrbot.image;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tesseract")
@Getter
public class TesseractProperties {

    @NotBlank
    private final String libPath;
    @NotBlank
    private final String dataPath;

    public TesseractProperties(final String libPath, final String dataPath) {
        this.libPath = libPath;
        this.dataPath = dataPath;
    }
}
