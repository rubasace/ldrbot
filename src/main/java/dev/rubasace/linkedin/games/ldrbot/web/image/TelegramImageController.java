package dev.rubasace.linkedin.games.ldrbot.web.image;

import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/images")
public class TelegramImageController {

    private final TelegramImageService telegramImageService;

    public TelegramImageController(TelegramImageService telegramImageService) {
        this.telegramImageService = telegramImageService;
    }

    @GetMapping("users/{userId}")
    public Resource getUserImage(@PathVariable Long userId) throws IOException {
        return telegramImageService.getUserImage(userId);
    }

}