package dev.rubasace.linkedin.games_tracker.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Transactional(readOnly = true)
@Service
public class TelegramUserService {

    private final TelegramUserRepository telegramUserRepository;

    public TelegramUserService(final TelegramUserRepository telegramUserRepository) {
        this.telegramUserRepository = telegramUserRepository;
    }

    @Transactional
    public TelegramUser findOrCreate(final Long userId, final String userName) {
        return telegramUserRepository.findById(userId)
                                     .map(telegramUser -> updateUserData(telegramUser, userName))
                                     .orElseGet(() -> this.createUser(userId, userName));
    }

    private TelegramUser updateUserData(TelegramUser telegramUser, final String userName) {
        if (telegramUser.getUserName().equals(userName)) {
            return telegramUser;
        }
        telegramUser.setUserName(userName);
        return telegramUserRepository.save(telegramUser);
    }

    private TelegramUser createUser(final Long userId, final String userName) {
        return telegramUserRepository.saveAndFlush(new TelegramUser(userId, userName, Set.of()));
    }
}
