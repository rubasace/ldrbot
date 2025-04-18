package dev.rubasace.linkedin.games_tracker;

import dev.rubasace.linkedin.games_tracker.group.TelegramGroup;
import dev.rubasace.linkedin.games_tracker.group.TelegramGroupRepository;
import dev.rubasace.linkedin.games_tracker.session.GameSession;
import dev.rubasace.linkedin.games_tracker.session.GameSessionRepository;
import dev.rubasace.linkedin.games_tracker.user.TelegramUser;
import dev.rubasace.linkedin.games_tracker.user.TelegramUserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    private final TelegramGroupRepository telegramGroupRepository;
    private final GameSessionRepository gameSessionRepository;
    private final TelegramUserRepository telegramUserRepository;

    public TestController(final TelegramGroupRepository telegramGroupRepository, final GameSessionRepository gameSessionRepository, final TelegramUserRepository telegramUserRepository) {
        this.telegramGroupRepository = telegramGroupRepository;
        this.gameSessionRepository = gameSessionRepository;
        this.telegramUserRepository = telegramUserRepository;
    }

    @GetMapping("groups")
    Iterable<TelegramGroup> getGroups() {
        return telegramGroupRepository.findAll();
    }


    @GetMapping("sessions")
    public Iterable<GameSession> getSessions() {
        return gameSessionRepository.findAll();
    }

    @GetMapping("users")
    public Iterable<TelegramUser> getUsers() {
        return telegramUserRepository.findAll();
    }
}
