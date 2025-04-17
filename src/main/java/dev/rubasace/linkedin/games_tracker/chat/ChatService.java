package dev.rubasace.linkedin.games_tracker.chat;

import dev.rubasace.linkedin.games_tracker.assets.AssetsDownloader;
import dev.rubasace.linkedin.games_tracker.group.TelegramGroup;
import dev.rubasace.linkedin.games_tracker.group.TelegramGroupService;
import dev.rubasace.linkedin.games_tracker.image.ImageGameDurationExtractor;
import dev.rubasace.linkedin.games_tracker.session.GameDuration;
import dev.rubasace.linkedin.games_tracker.session.GameSessionService;
import dev.rubasace.linkedin.games_tracker.user.TelegramUser;
import dev.rubasace.linkedin.games_tracker.user.TelegramUserService;
import dev.rubasace.linkedin.games_tracker.util.FormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
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
    void setupGroup(final Message message) {
        TelegramGroup telegramGroup = telegramGroupService.registerOrUpdateGroup(message.getChat());
        sendMessage("Group %s(%d) registered correctly".formatted(telegramGroup.getGroupName(), telegramGroup.getChatId()), message.getChatId());
    }

    @Transactional
    void addUserToGroup(final Message message) {
        Optional<TelegramGroup> group = telegramGroupService.findGroup(message.getChatId());
        if (group.isEmpty()) {
            sendMessage("Group not registered. Must execute /start command first", message.getChatId());
            return;
        }
        TelegramUser telegramUser = telegramUserService.findOrCreate(message.getFrom());
        group.get().getMembers().add(telegramUser);
        telegramGroupService.save(group.get());
        sendMessage("User @%s joined this group".formatted(telegramUser.getUserName()), message.getChatId());
    }

    void processPhoto(final List<PhotoSize> photoSizeList, final String username, final Long chatId) {

        File imageFile = assetsDownloader.getImage(photoSizeList);
        Optional<GameDuration> gameDuration = imageGameDurationExtractor.extractGameDuration(imageFile);
        if (gameDuration.isEmpty()) {
            return;
        }
        sendMessage(SUBMISSION_MESSAGE_TEMPLATE.formatted(username, gameDuration.get().type().name(), FormatUtils.formatDuration(gameDuration.get().duration())), chatId);

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
