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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.File;
import java.util.List;
import java.util.Optional;

@Transactional(readOnly = true)
@Service
class ChatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatService.class);
    public static final String SUBMISSION_MESSAGE_TEMPLATE = "@%s submitted a screenshot for todays %s taking a total time of %s";

    private final ImageGameDurationExtractor imageGameDurationExtractor;
    private final AssetsDownloader assetsDownloader;
    private final GameSessionService gameSessionService;
    private final TelegramGroupService telegramGroupService;
    private final TelegramUserService telegramUserService;
    private final TelegramClient telegramClient;

    ChatService(final ImageGameDurationExtractor imageGameDurationExtractor, final AssetsDownloader assetsDownloader, final GameSessionService gameSessionService, final TelegramGroupService telegramGroupService, final TelegramClient telegramClient, final TelegramUserService telegramUserService) {
        this.imageGameDurationExtractor = imageGameDurationExtractor;
        this.assetsDownloader = assetsDownloader;
        this.gameSessionService = gameSessionService;
        this.telegramGroupService = telegramGroupService;
        this.telegramClient = telegramClient;
        this.telegramUserService = telegramUserService;
    }

    @Transactional
    void setupGroup(final Chat chat) {
        telegramGroupService.registerOrUpdateGroup(chat);
        sendMessage("Now I'll monitor this group and keep track of your linkedin games times. You can /join or start sending your screenshots", chat.getId());
    }

    @Transactional
    void addUserToGroup(final Long chatId, final User user) {
        Optional<TelegramGroup> group = telegramGroupService.findGroup(chatId);
        if (group.isEmpty()) {
            sendMessage("Group not registered. Must execute /start command first", chatId);
            return;
        }
        TelegramUser telegramUser = telegramUserService.findOrCreate(user);
        if (group.get().getMembers().contains(telegramUser)) {
            return;
        }
        group.get().getMembers().add(telegramUser);
        telegramGroupService.save(group.get());
        sendMessage("User @%s joined this group".formatted(telegramUser.getUserName()), chatId);
    }

    //TODO improve this and make it photo specific using the message. Override should also accept documents same as normal messages
    @Transactional
    void processMessage(final List<PhotoSize> photoSizeList, final User user, final Chat chat) {
        if (chat.isGroupChat()) {
            Optional<TelegramGroup> group = telegramGroupService.findGroup(chat.getId());
            if (group.isEmpty()) {
                return;
            }
            addUserToGroup(chat.getId(), user);
        }

        File imageFile = assetsDownloader.getImage(photoSizeList);
        Optional<GameDuration> gameDuration = imageGameDurationExtractor.extractGameDuration(imageFile);
        if (gameDuration.isEmpty()) {
            return;
        }
        try {
            GameSession gameSession = gameSessionService.recordGameSession(user, gameDuration.get());
            sendMessage(SUBMISSION_MESSAGE_TEMPLATE.formatted(gameSession.getTelegramUser().getUserName(), gameSession.getGame().name(),
                                                              FormatUtils.formatDuration(gameSession.getDuration())),
                        chat.getId());
        } catch (AlreadyRegisteredSession e) {
            sendMessage(
                    "@%s already registered a time for %s. If you need to override the time, please delete the current time through the \"/delete <game>\" command. In this case: /delete %s".formatted(
                            e.getUsername(), e.getGame().name(), e.getGame().name().toLowerCase()), chat.getId());
        }

    }

    @Transactional
    public void deleteGameRecord(final Message message, final String gameName) throws UnrecognizedGameException {
        try {
            GameType gameType = GameType.valueOf(gameName.toUpperCase());
            gameSessionService.deleteTodaysSession(message.getFrom(), gameType);
        } catch (IllegalArgumentException e) {
            throw new UnrecognizedGameException(gameName);
        }
    }

    private void sendMessage(final String text, final Long chatId) {
        SendMessage message = SendMessage.builder()
                                         .chatId(chatId)
                                         .text(text)
                                         .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            LOGGER.error("Error sending message", e);
        }
    }

}
