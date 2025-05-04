package dev.rubasace.linkedin.games.ldrbot.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Transactional(readOnly = true)
@Service
public class TelegramUserService {

    private final TelegramUserRepository telegramUserRepository;
    private final TelegramUserMapper telegramUserMapper;

    TelegramUserService(final TelegramUserRepository telegramUserRepository, final TelegramUserMapper telegramUserMapper) {
        this.telegramUserRepository = telegramUserRepository;
        this.telegramUserMapper = telegramUserMapper;
    }

    public Optional<TelegramUser> find(final UserInfo userInfo) {
        return telegramUserRepository.findById(userInfo.id());
    }

    @Transactional
    public TelegramUser findOrCreate(final UserInfo userInfo) {
        return telegramUserRepository.findById(userInfo.id())
                                     .map(telegramUser -> updateUserData(telegramUser, userInfo))
                                     .orElseGet(() -> this.createUser(userInfo));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Stream<MissingSessionUserProjection> findUsersWithMissingSessions(final LocalDate gameDay) {
        return telegramUserRepository.findUsersWithMissingSessions(gameDay);
    }

    public Optional<TelegramUser> findByUserName(final String userName) {
        return telegramUserRepository.findByUserName(userName);
    }

    private TelegramUser updateUserData(TelegramUser telegramUser, final UserInfo userInfo) {
        if (Objects.equals(telegramUser.getUserName(), userInfo.userName())
                && Objects.equals(telegramUser.getFirstName(), userInfo.firstName())
                && Objects.equals(telegramUser.getLastName(), userInfo.lastName())) {
            return telegramUser;
        }
        telegramUser.setUserName(userInfo.userName());
        telegramUser.setFirstName(userInfo.firstName());
        telegramUser.setLastName(userInfo.lastName());
        return telegramUserRepository.save(telegramUser);
    }

    private TelegramUser createUser(final UserInfo userInfo) {
        return telegramUserRepository.saveAndFlush(telegramUserMapper.map(userInfo));
    }
}
