package dev.rubasace.linkedin.games_tracker.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.Set;

@Transactional(readOnly = true)
@Service
public class TelegramUserService {

    private final TelegramUserRepository telegramUserRepository;

    public TelegramUserService(final TelegramUserRepository telegramUserRepository) {
        this.telegramUserRepository = telegramUserRepository;
    }

    @Transactional
    public TelegramUser findOrCreate(final User user) {
        return telegramUserRepository.findById(user.getId())
                                     .map(telegramUser -> updateUserData(telegramUser, user))
                                     .orElseGet(() -> this.createUser(user));
    }

    private TelegramUser updateUserData(TelegramUser telegramUser, final User user) {
        if (telegramUser.getUserName().equals(user.getUserName())) {
            return telegramUser;
        }
        telegramUser.setUserName(user.getUserName());
        return telegramUserRepository.save(telegramUser);
    }

    private TelegramUser createUser(final User user) {
        TelegramUser telegramUser = new TelegramUser(user.getId(), user.getUserName(), user.getFirstName(), user.getLastName(), Set.of());
        return telegramUserRepository.save(telegramUser);
    }
}
