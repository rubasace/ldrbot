package dev.rubasace.linkedin.games.ldrbot.message;

import dev.rubasace.linkedin.games.ldrbot.assets.AssetsDownloader;
import dev.rubasace.linkedin.games.ldrbot.configuration.TelegramBotProperties;
import dev.rubasace.linkedin.games.ldrbot.group.ChatInfo;
import dev.rubasace.linkedin.games.ldrbot.group.GroupNotFoundException;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroup;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupService;
import dev.rubasace.linkedin.games.ldrbot.image.GameDurationExtractionException;
import dev.rubasace.linkedin.games.ldrbot.image.ImageGameDurationExtractor;
import dev.rubasace.linkedin.games.ldrbot.session.GameDuration;
import dev.rubasace.linkedin.games.ldrbot.session.GameSessionService;
import dev.rubasace.linkedin.games.ldrbot.session.SessionAlreadyRegisteredException;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import dev.rubasace.linkedin.games.ldrbot.util.LinkedinTimeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional(readOnly = true)
@Service
class MessageService {

    private final ImageGameDurationExtractor imageGameDurationExtractor;
    private final AssetsDownloader assetsDownloader;
    private final GameSessionService gameSessionService;
    private final TelegramGroupService telegramGroupService;
    private final TelegramBotProperties telegramBotProperties;
    private final Map<String, BotCommand> knownCommands;
    private final UserAdapter userAdapter;
    private final ChatAdapter chatAdapter;

    MessageService(final ImageGameDurationExtractor imageGameDurationExtractor, final AssetsDownloader assetsDownloader, final GameSessionService gameSessionService, final TelegramGroupService telegramGroupService, final TelegramBotProperties telegramBotProperties, final Map<String, BotCommand> knownCommands, final UserAdapter userAdapter, final ChatAdapter chatAdapter) {
        this.imageGameDurationExtractor = imageGameDurationExtractor;
        this.assetsDownloader = assetsDownloader;
        this.gameSessionService = gameSessionService;
        this.telegramGroupService = telegramGroupService;
        this.telegramBotProperties = telegramBotProperties;
        this.knownCommands = knownCommands;
        this.userAdapter = userAdapter;
        this.chatAdapter = chatAdapter;
    }


    void registerCommands(final List<BotCommand> abilities) {
        knownCommands.putAll(abilities.stream()
                                      .collect(Collectors.toMap(command -> "/" + command.getCommand(), command -> command)));
    }

    @Transactional
    TelegramGroup registerOrUpdateGroup(final Message message) {
        ChatInfo chatInfo = chatAdapter.adapt(message.getChat());
        return telegramGroupService.registerOrUpdateGroup(chatInfo);
    }

    @Transactional
    void addUserToGroup(final Message message) throws GroupNotFoundException {
        ChatInfo chatInfo = chatAdapter.adapt(message.getChat());
        UserInfo userInfo = userAdapter.adapt(message.getFrom());
        telegramGroupService.addUserToGroup(chatInfo, userInfo);
    }

    @Transactional
    void processMessage(final Message message) throws UnknownCommandException, GameDurationExtractionException, SessionAlreadyRegisteredException, GroupNotFoundException {

        String receivedCommand = message.getText() != null ? message.getText().split("[\\s@]")[0] : "";
        if (message.isCommand() && !knownCommands.containsKey(receivedCommand)) {
            throw new UnknownCommandException(message.getChatId(), message.getText());
        }

        if (message.getChat().isGroupChat()) {
            processGroupMessage(message);
        } else {
            processPrivateMessage(message);
        }
    }

    private void processPrivateMessage(final Message message) {
        //Do nothing for now
    }

    private void processGroupMessage(final Message message) throws GroupNotFoundException, SessionAlreadyRegisteredException, GameDurationExtractionException {
        ChatInfo chatInfo = chatAdapter.adapt(message.getChat());
        if (isBotRemovedFromGroup(message)) {
            telegramGroupService.removeGroup(chatInfo);
            return;
        }
        addUserToGroup(message);
        if (!CollectionUtils.isEmpty(message.getNewChatMembers())) {
            for (User user : message.getNewChatMembers()) {
                if (!telegramBotProperties.getUsername().equalsIgnoreCase(user.getUserName())) {
                    telegramGroupService.addUserToGroup(chatInfo, userAdapter.adapt(user));
                }
            }
            return;
        }
        if (message.getLeftChatMember() != null) {
            UserInfo userInfo = userAdapter.adapt(message.getLeftChatMember());
            telegramGroupService.removeUserFromGroup(chatInfo, userInfo);
            return;
        }

        List<PhotoSize> photoSizeList = getPhotos(message);
        if (photoSizeList.isEmpty()) {
            return;
        }

        UserInfo userInfo = userAdapter.adapt(message.getFrom());
        File imageFile = assetsDownloader.getImage(photoSizeList);
        Optional<GameDuration> gameDuration = imageGameDurationExtractor.extractGameDuration(imageFile, message.getChatId(), userInfo);
        if (gameDuration.isEmpty()) {
            return;
        }
        gameSessionService.recordGameSession(chatInfo, userInfo, gameDuration.get(), LinkedinTimeUtils.todayGameDay());
    }

    private boolean isBotRemovedFromGroup(final Message message) {
        return message.getLeftChatMember() != null && message.getLeftChatMember().getUserName().equalsIgnoreCase(telegramBotProperties.getUsername());
    }

    private List<PhotoSize> getPhotos(final Message message) {
        if (message.hasPhoto()) {
            return message.getPhoto();
        } else if (message.hasDocument() && message.getDocument().getThumbnail() != null) {
            return List.of(message.getDocument().getThumbnail());
        }
        return List.of();
    }

}
