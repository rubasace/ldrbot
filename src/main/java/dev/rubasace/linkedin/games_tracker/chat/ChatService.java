package dev.rubasace.linkedin.games_tracker.chat;

import dev.rubasace.linkedin.games_tracker.assets.AssetsDownloader;
import dev.rubasace.linkedin.games_tracker.group.TelegramGroup;
import dev.rubasace.linkedin.games_tracker.group.TelegramGroupService;
import dev.rubasace.linkedin.games_tracker.image.ImageGameDurationExtractor;
import dev.rubasace.linkedin.games_tracker.session.AlreadyRegisteredSession;
import dev.rubasace.linkedin.games_tracker.session.GameDuration;
import dev.rubasace.linkedin.games_tracker.session.GameSession;
import dev.rubasace.linkedin.games_tracker.session.GameSessionService;
import dev.rubasace.linkedin.games_tracker.session.GameType;
import dev.rubasace.linkedin.games_tracker.session.UnrecognizedGameException;
import dev.rubasace.linkedin.games_tracker.user.TelegramUser;
import dev.rubasace.linkedin.games_tracker.user.TelegramUserService;
import dev.rubasace.linkedin.games_tracker.util.FormatUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.io.File;
import java.util.List;
import java.util.Optional;

@Transactional(readOnly = true)
@Service
class ChatService {

    public static final String SUBMISSION_MESSAGE_TEMPLATE = "@%s submitted a screenshot for todays %s taking a total time of %s";

    private final ImageGameDurationExtractor imageGameDurationExtractor;
    private final AssetsDownloader assetsDownloader;
    private final GameSessionService gameSessionService;
    private final TelegramGroupService telegramGroupService;
    private final TelegramUserService telegramUserService;
    private final MessageService messageService;

    ChatService(final ImageGameDurationExtractor imageGameDurationExtractor,
                final AssetsDownloader assetsDownloader,
                final GameSessionService gameSessionService,
                final TelegramGroupService telegramGroupService,
                final MessageService messageService, final TelegramUserService telegramUserService) {
        this.imageGameDurationExtractor = imageGameDurationExtractor;
        this.assetsDownloader = assetsDownloader;
        this.gameSessionService = gameSessionService;
        this.telegramGroupService = telegramGroupService;
        this.messageService = messageService;
        this.telegramUserService = telegramUserService;
    }

    @Transactional
    void setupGroup(final Chat chat) {
        if (!chat.isGroupChat()) {
            throw new IllegalArgumentException("Chat must be a group");
        }
        telegramGroupService.registerOrUpdateGroup(chat.getId(), chat.getTitle());
        messageService.info("Now I'll monitor this group and keep track of your linkedin games times. You can explicitely /join or send a message on the group to be added to it",
                            chat.getId());
    }

    @Transactional
    void addUserToGroup(final Message message) {
        Optional<TelegramGroup> group = telegramGroupService.findGroup(message.getChatId());
        if (group.isEmpty()) {
            messageService.error("Group not registered. Must execute /start command first", message.getChatId());
            return;
        }
        TelegramUser telegramUser = telegramUserService.findOrCreate(message.getFrom().getId(), message.getFrom().getUserName());
        if (group.get().getMembers().contains(telegramUser)) {
            return;
        }
        group.get().getMembers().add(telegramUser);
        telegramGroupService.save(group.get());
        messageService.info("User @%s joined this group".formatted(telegramUser.getUserName()), message.getChatId());
    }

    @Transactional
    void processMessage(final Message message) {

        if (message.getChat().isGroupChat()) {
            Optional<TelegramGroup> group = telegramGroupService.findGroup(message.getChat().getId());
            if (group.isEmpty()) {
                return;
            }
            addUserToGroup(message);
        }

        List<PhotoSize> photoSizeList = getPhotos(message);
        if (photoSizeList.isEmpty()) {
            return;
        }

        File imageFile = assetsDownloader.getImage(photoSizeList);
        Optional<GameDuration> gameDuration = imageGameDurationExtractor.extractGameDuration(imageFile);
        if (gameDuration.isEmpty()) {
            return;
        }
        try {
            GameSession gameSession = gameSessionService.recordGameSession(message.getFrom().getId(), message.getFrom().getUserName(), gameDuration.get());
            messageService.info(SUBMISSION_MESSAGE_TEMPLATE.formatted(gameSession.getTelegramUser().getUserName(), gameSession.getGame().name(),
                                                              FormatUtils.formatDuration(gameSession.getDuration())),
                                message.getChat().getId());
        } catch (AlreadyRegisteredSession e) {
            messageService.error(
                    "@%s already registered a time for %s. If you need to override the time, please delete the current time through the \"/delete <game>\" command. In this case: /delete %s. Alternatively, you can delete all your submissions for the day using /deleteall".formatted(
                            e.getUsername(), e.getGame().name(), e.getGame().name().toLowerCase()), message.getChat().getId());
        }

    }

    private List<PhotoSize> getPhotos(final Message message) {
        if (message.hasPhoto()) {
            return message.getPhoto();
        } else if (message.hasDocument() && message.getDocument().getThumbnail() != null) {
            return List.of(message.getDocument().getThumbnail());
        }
        return List.of();
    }

    @Transactional
    public void deleteTodayRecord(final Message message, final String gameName) throws UnrecognizedGameException {
        GameType gameType;
        try {
            gameType = GameType.valueOf(gameName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new UnrecognizedGameException(gameName);
        }
        gameSessionService.deleteTodaySession(message.getFrom().getId(), gameType);
    }

    @Transactional
    public void deleteTodayRecords(final Message message) {
        gameSessionService.deleteTodaySessions(message.getFrom().getId());
    }

}
