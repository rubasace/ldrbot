package dev.rubasace.linkedin.games.ldrbot.message;

import dev.rubasace.linkedin.games.ldrbot.assets.AssetsDownloader;
import dev.rubasace.linkedin.games.ldrbot.chat.ChatService;
import dev.rubasace.linkedin.games.ldrbot.configuration.TelegramBotProperties;
import dev.rubasace.linkedin.games.ldrbot.exception.HandleBotExceptions;
import dev.rubasace.linkedin.games.ldrbot.group.GroupNotFoundException;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroup;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupService;
import dev.rubasace.linkedin.games.ldrbot.image.GameDurationExtractionException;
import dev.rubasace.linkedin.games.ldrbot.image.ImageGameDurationExtractor;
import dev.rubasace.linkedin.games.ldrbot.ranking.GroupRankingService;
import dev.rubasace.linkedin.games.ldrbot.session.AlreadyRegisteredSession;
import dev.rubasace.linkedin.games.ldrbot.session.GameDuration;
import dev.rubasace.linkedin.games.ldrbot.session.GameNameNotFoundException;
import dev.rubasace.linkedin.games.ldrbot.session.GameSessionService;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.util.LinkedinTimeUtils;
import dev.rubasace.linkedin.games.ldrbot.util.ParseUtils;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
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

    MessageService(final ImageGameDurationExtractor imageGameDurationExtractor, final AssetsDownloader assetsDownloader, final GameSessionService gameSessionService, final TelegramGroupService telegramGroupService, final ChatService chatService, final GroupRankingService groupRankingService, final TelegramBotProperties telegramBotProperties) {
        this.imageGameDurationExtractor = imageGameDurationExtractor;
        this.assetsDownloader = assetsDownloader;
        this.gameSessionService = gameSessionService;
        this.telegramGroupService = telegramGroupService;
        this.chatService = chatService;
        this.groupRankingService = groupRankingService;
        this.telegramBotProperties = telegramBotProperties;
        knownCommands = new HashMap<>();
    }

    void registerCommands(final List<BotCommand> abilities) {
        knownCommands.putAll(abilities.stream()
                                      .collect(Collectors.toMap(command1 -> "/" + command1.getCommand(), command -> command)));
    }

    @Transactional
    TelegramGroup registerOrUpdateGroup(final Long chatId, final String title) {
        return telegramGroupService.registerOrUpdateGroup(chatId, title);
    }

    @SneakyThrows
    @Transactional
    void addUserToGroup(final Message message) {
        telegramGroupService.addUserToGroup(message.getChatId(), message.getFrom().getId(), message.getFrom().getUserName());
    }

    @SneakyThrows
    @Transactional
    void processMessage(final Message message) {

        if (message.isCommand() && !knownCommands.containsKey(message.getText().split(" ")[0])) {
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

    private void processGroupMessage(final Message message) throws GroupNotFoundException, AlreadyRegisteredSession, GameDurationExtractionException {
        if (isBotRemovedFromGroup(message)) {
            telegramGroupService.removeGroup(message.getChatId());
            return;
        }
        addUserToGroup(message);
        if (!CollectionUtils.isEmpty(message.getNewChatMembers())) {
            for (User user : message.getNewChatMembers()) {
                if (!user.getUserName().equalsIgnoreCase(telegramBotProperties.getUsername())) {
                    telegramGroupService.addUserToGroup(message.getChatId(), user.getId(), user.getUserName());
                }
            }
            return;
        }
        if (message.getLeftChatMember() != null) {
            telegramGroupService.removeUserFromGroup(message.getChatId(), message.getLeftChatMember().getId());
            return;
        }

        List<PhotoSize> photoSizeList = getPhotos(message);
        if (photoSizeList.isEmpty()) {
            return;
        }

        File imageFile = assetsDownloader.getImage(photoSizeList);
        Optional<GameDuration> gameDuration = imageGameDurationExtractor.extractGameDuration(imageFile, message.getChatId(), message.getFrom().getUserName());
        if (gameDuration.isEmpty()) {
            return;
        }
        gameSessionService.recordGameSession(message.getFrom().getId(), message.getChatId(), message.getFrom().getUserName(), gameDuration.get());
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
        GameType gameType = getGameType(gameName, message.getChatId());
        gameSessionService.deleteTodaySession(message.getFrom().getId(), message.getChatId(), gameType);
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
        gameSessionService.deleteDaySessions(message.getFrom().getId(), message.getChatId(), LinkedinTimeUtils.todayGameDay());
    }

    @Transactional
    public void dailyRanking(final Message message) {
        telegramGroupService.findGroup(message.getChat().getId()).ifPresent(
                telegramGroup -> groupRankingService.createDailyRanking(telegramGroup, LinkedinTimeUtils.todayGameDay()));
    }

    @SneakyThrows
    public void listTrackedGames(final Message message) {
        chatService.listTrackedGames(message.getChatId());
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
        gameSessionService.manuallRrecordGameSession(message.getChatId(), username, new GameDuration(gameType, duration.get()));
    }

    public void help(final Message message) {
        chatService.help(message.getChatId(), knownCommands);
    }

    public void privateStart(final Message message) {
        chatService.privateStart(message.getChatId());
    }
}
