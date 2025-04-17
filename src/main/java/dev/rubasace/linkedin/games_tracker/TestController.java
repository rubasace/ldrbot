package dev.rubasace.linkedin.games_tracker;

import dev.rubasace.linkedin.games_tracker.group.TelegramGroup;
import dev.rubasace.linkedin.games_tracker.group.TelegramGroupRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("groups")
@RestController
public class TestController {

    private final TelegramGroupRepository telegramGroupRepository;

    public TestController(final TelegramGroupRepository telegramGroupRepository) {
        this.telegramGroupRepository = telegramGroupRepository;
    }

    @GetMapping
    Iterable<TelegramGroup> getGroups() {
        return telegramGroupRepository.findAll();
    }
}
