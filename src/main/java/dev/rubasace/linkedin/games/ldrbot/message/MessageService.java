package dev.rubasace.linkedin.games.ldrbot.message;

import dev.rubasace.linkedin.games.ldrbot.assets.AssetsDownloader;
import dev.rubasace.linkedin.games.ldrbot.chat.ChatService;
import dev.rubasace.linkedin.games.ldrbot.configuration.TelegramBotProperties;
import dev.rubasace.linkedin.games.ldrbot.exception.HandleBotExceptions;
import dev.rubasace.linkedin.games.ldrbot.group.GroupInfo;
import dev.rubasace.linkedin.games.ldrbot.group.GroupNotFoundException;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroup;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupService;
import dev.rubasace.linkedin.games.ldrbot.image.GameDurationExtractionException;
import dev.rubasace.linkedin.games.ldrbot.image.ImageGameDurationExtractor;
import dev.rubasace.linkedin.games.ldrbot.ranking.GroupRankingService;
import dev.rubasace.linkedin.games.ldrbot.session.GameDuration;
import dev.rubasace.linkedin.games.ldrbot.session.GameNameNotFoundException;
import dev.rubasace.linkedin.games.ldrbot.session.GameSessionService;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.session.SessionAlreadyRegisteredException;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUser;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUserService;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import dev.rubasace.linkedin.games.ldrbot.user.UserNotFoundException;
import dev.rubasace.linkedin.games.ldrbot.util.LinkedinTimeUtils;
import dev.rubasace.linkedin.games.ldrbot.util.ParseUtils;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

//TODO update bot description on startup
@HandleBotExceptions
@Transactional(readOnly = true)
@Service
class MessageService {

    private final ImageGameDurationExtractor imageGameDurationExtractor;
    private final AssetsDownloader assetsDownloader;
    private final GameSessionService gameSessionService;
    private final TelegramGroupService telegramGroupService;
    private final ChatService chatService;
    private final GroupRankingService groupRankingService;
    private final TelegramBotProperties telegramBotProperties;
    private final Map<String, BotCommand> knownCommands;
    private final UserInfoAdapter userInfoAdapter;
    private final GroupInfoAdapter groupInfoAdapter;
    private final TelegramUserService telegramUserService;

    MessageService(final ImageGameDurationExtractor imageGameDurationExtractor, final AssetsDownloader assetsDownloader, final GameSessionService gameSessionService, final TelegramGroupService telegramGroupService, final ChatService chatService, final GroupRankingService groupRankingService, final TelegramBotProperties telegramBotProperties, final UserInfoAdapter userInfoAdapter, final GroupInfoAdapter groupInfoAdapter, final TelegramUserService telegramUserService) {
        this.imageGameDurationExtractor = imageGameDurationExtractor;
        this.assetsDownloader = assetsDownloader;
        this.gameSessionService = gameSessionService;
        this.telegramGroupService = telegramGroupService;
        this.chatService = chatService;
        this.groupRankingService = groupRankingService;
        this.telegramBotProperties = telegramBotProperties;
        this.userInfoAdapter = userInfoAdapter;
        this.groupInfoAdapter = groupInfoAdapter;
        knownCommands = new HashMap<>();
        this.telegramUserService = telegramUserService;
    }

    void registerCommands(final List<BotCommand> abilities) {
        knownCommands.putAll(abilities.stream()
                                      .collect(Collectors.toMap(command1 -> "/" + command1.getCommand(), command -> command)));
    }

    @Transactional
    TelegramGroup registerOrUpdateGroup(final Message message) {
        GroupInfo groupInfo = groupInfoAdapter.adapt(message.getChat());
        return telegramGroupService.registerOrUpdateGroup(groupInfo);
    }

    @SneakyThrows
    @Transactional
    void addUserToGroup(final Message message) {
        GroupInfo groupInfo = groupInfoAdapter.adapt(message.getChat());
        UserInfo userInfo = userInfoAdapter.adapt(message.getFrom());
        telegramGroupService.addUserToGroup(groupInfo, userInfo);
    }

    @SneakyThrows
    @Transactional
    void processMessage(final Message message) {

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
        GroupInfo groupInfo = groupInfoAdapter.adapt(message.getChat());
        if (isBotRemovedFromGroup(message)) {
            telegramGroupService.removeGroup(groupInfo);
            return;
        }
        addUserToGroup(message);
        if (!CollectionUtils.isEmpty(message.getNewChatMembers())) {
            for (User user : message.getNewChatMembers()) {
                if (!telegramBotProperties.getUsername().equalsIgnoreCase(user.getUserName())) {
                    telegramGroupService.addUserToGroup(groupInfo, userInfoAdapter.adapt(user));
                }
            }
            return;
        }
        if (message.getLeftChatMember() != null) {
            UserInfo userInfo = userInfoAdapter.adapt(message.getLeftChatMember());
            telegramGroupService.removeUserFromGroup(groupInfo, userInfo);
            return;
        }

        List<PhotoSize> photoSizeList = getPhotos(message);
        if (photoSizeList.isEmpty()) {
            return;
        }

        UserInfo userInfo = userInfoAdapter.adapt(message.getFrom());
        File imageFile = assetsDownloader.getImage(photoSizeList);
        Optional<GameDuration> gameDuration = imageGameDurationExtractor.extractGameDuration(imageFile, message.getChatId(), userInfo);
        if (gameDuration.isEmpty()) {
            return;
        }
        gameSessionService.recordGameSession(groupInfo, userInfo, gameDuration.get());
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

    @SneakyThrows
    @Transactional
    public void deleteTodayRecord(final Message message, final String[] arguments) {
        //TODO think if controlling actions arguments with input() or via explicit arguments check like here. Probably here so we have more control over everything
        if (arguments == null || arguments.length == 0) {
            throw new InvalidUserInputException("Please provide a game name. Example: /delete queens", message.getChatId());
        }

        String gameName = arguments[0];
        GroupInfo groupInfo = groupInfoAdapter.adapt(message.getChat());
        UserInfo userInfo = userInfoAdapter.adapt(message.getFrom());
        GameType gameType = getGameType(gameName, message.getChatId());
        gameSessionService.deleteTodaySession(groupInfo, userInfo, gameType);
    }

    private GameType getGameType(final String gameName, final Long chatId) throws GameNameNotFoundException {
        GameType gameType;
        try {
            gameType = GameType.valueOf(gameName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new GameNameNotFoundException(chatId, gameName);
        }
        return gameType;
    }

    @Transactional
    public void deleteTodayRecords(final Message message) {
        GroupInfo groupInfo = groupInfoAdapter.adapt(message.getChat());
        UserInfo userInfo = userInfoAdapter.adapt(message.getFrom());
        gameSessionService.deleteDaySessions(groupInfo, userInfo, LinkedinTimeUtils.todayGameDay());
    }

    @Transactional
    public void dailyRanking(final Message message) {
        telegramGroupService.findGroup(message.getChat().getId()).ifPresent(
                telegramGroup -> groupRankingService.createDailyRanking(telegramGroup, LinkedinTimeUtils.todayGameDay()));
    }

    @SneakyThrows
    public void listTrackedGames(final Message message) {
        GroupInfo groupInfo = groupInfoAdapter.adapt(message.getChat());
        chatService.listTrackedGames(groupInfo);
    }

    @Transactional
    @SneakyThrows
    public void registerSessionManually(final Message message, final String[] arguments) {
        String username = arguments[0].startsWith("@") ? arguments[0].substring(1) : arguments[0];
        GameType gameType = getGameType(arguments[1], message.getChatId());
        Optional<Duration> duration = ParseUtils.parseDuration(arguments[2]);
        if (duration.isEmpty()) {
            throw new InvalidUserInputException("Invalid time format. Use `mm:ss`, e.g. `1:30` or `12:05`", message.getChatId());
        }
        GroupInfo groupInfo = groupInfoAdapter.adapt(message.getChat());
        Optional<User> mentionedUser = getMentionedUser(message);
        if (mentionedUser.isPresent()) {
            UserInfo userInfo = userInfoAdapter.adapt(mentionedUser.get());
            gameSessionService.recordGameSession(groupInfo, userInfo, new GameDuration(gameType, duration.get()));
        } else {
            TelegramUser telegramUser = telegramUserService.findByUserName(username)
                                                           .orElseThrow(() -> new UserNotFoundException(message.getChatId(), new UserInfo(null, username, "", "")));
            //TODO move to adapter
            UserInfo userInfo = new UserInfo(telegramUser.getId(), telegramUser.getUserName(), telegramUser.getFirstName(), telegramUser.getLastName());
            gameSessionService.recordGameSession(groupInfo, userInfo, new GameDuration(gameType, duration.get()));
        }
    }

    private Optional<User> getMentionedUser(final Message message) {
        return Optional.ofNullable(message.getEntities())
                       .flatMap(entities -> entities.stream()
                                                    .filter(entity -> "text_mention".equals(entity.getType()))
                                                    .findFirst()
                                                    .map(MessageEntity::getUser));

    }

    public void help(final Message message) {
        chatService.help(message.getChatId(), knownCommands);
    }

    public void start(final Message message) {
        if (message.getChat().isGroupChat()) {
            chatService.groupStart(message.getChatId());
        } else {
            chatService.privateStart(message.getChatId());
        }
    }
}
