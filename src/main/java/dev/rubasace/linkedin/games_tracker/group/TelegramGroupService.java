//package dev.rubasace.linkedin.games_tracker.group;
//
//import dev.rubasace.linkedin.games_tracker.user.TelegramUser;
//import dev.rubasace.linkedin.games_tracker.user.TelegramUserRepository;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.telegram.telegrambots.meta.api.objects.User;
//
//@Transactional(readOnly = true)
//@Service
//public class TelegramGroupService {
//
//    private final TelegramUserRepository telegramUserRepository;
//
//    public TelegramGroupService(final TelegramUserRepository telegramUserRepository) {
//        this.telegramUserRepository = telegramUserRepository;
//    }
//
//    @Transactional
//    public TelegramGroup findOrCreate(final User user){
//        return telegramUserRepository.findById(user.getId())
//                .map(telegramUser -> updateUserData(telegramUser, user))
//                .orElseGet(() -> this.createUser(user));
//    }
//
//    private TelegramUser updateUserData(TelegramUser telegramUser, final User user) {
//        if (telegramUser.getUserName().equals(user.getUserName())) {
//            return telegramUser;
//        }
//        telegramUser.setUserName(user.getUserName());
//        return telegramUserRepository.save(telegramUser);
//    }
//
//    private TelegramUser createUser(final User user) {
//        TelegramUser telegramUser = new TelegramUser(user.getId(), user.getUserName(), user.getFirstName(), user.getLastName());
//        return telegramUserRepository.save(telegramUser);
//    }
//}
