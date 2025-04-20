package dev.rubasace.linkedin.games_tracker.chat;

import dev.rubasace.linkedin.games_tracker.assets.AssetsDownloader;
import dev.rubasace.linkedin.games_tracker.exception.HandleBotExceptions;
import dev.rubasace.linkedin.games_tracker.group.GroupNotFoundException;
import dev.rubasace.linkedin.games_tracker.group.TelegramGroup;
import dev.rubasace.linkedin.games_tracker.group.TelegramGroupService;
import dev.rubasace.linkedin.games_tracker.image.ImageGameDurationExtractor;
import dev.rubasace.linkedin.games_tracker.ranking.GroupRankingService;
import dev.rubasace.linkedin.games_tracker.session.GameDuration;
import dev.rubasace.linkedin.games_tracker.session.GameSession;
import dev.rubasace.linkedin.games_tracker.session.GameSessionService;
import dev.rubasace.linkedin.games_tracker.session.GameType;
import dev.rubasace.linkedin.games_tracker.util.FormatUtils;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@HandleBotExceptions
//TODO revisit if it makes sense after we revisit all logic in this service
@Transactional(readOnly = true)
@Service
class ChatService {

    public static final String SUBMISSION_MESSAGE_TEMPLATE = "@%s submitted a screenshot for todays %s with a time of %s";

    private final ImageGameDurationExtractor imageGameDurationExtractor;
    private final AssetsDownloader assetsDownloader;
    private final GameSessionService gameSessionService;
    private final TelegramGroupService telegramGroupService;
    private final MessageService messageService;
    private final GroupRankingService groupRankingService;

    ChatService(final ImageGameDurationExtractor imageGameDurationExtractor,
                final AssetsDownloader assetsDownloader,
                final GameSessionService gameSessionService,
                final TelegramGroupService telegramGroupService,
                final MessageService messageService,
                final GroupRankingService groupRankingService) {
        this.imageGameDurationExtractor = imageGameDurationExtractor;
        this.assetsDownloader = assetsDownloader;
        this.gameSessionService = gameSessionService;
        this.telegramGroupService = telegramGroupService;
        this.messageService = messageService;
        this.groupRankingService = groupRankingService;
    }

    @Transactional
    void setupGroup(final Chat chat) {
        if (!chat.isGroupChat()) {
            throw new IllegalArgumentException("Chat must be a group");
        }
        telegramGroupService.registerOrUpdateGroup(chat.getId(), chat.getTitle());
        messageService.info("Now I'll monitor this group and keep track of your linkedin games times. You can explicitly /join or send a message on the group to be added to it",
                            chat.getId());
    }

    //TODO revisit if SneakyThrows makes sense here, probably will be kept if logic extracted into specific action component
    @SneakyThrows
    @Transactional
    void addUserToGroup(final Message message) {
        boolean joined = telegramGroupService.addUserToGroup(message.getChatId(), message.getFrom().getId(), message.getFrom().getUserName());
        if (joined) {
            messageService.info("User @%s joined this group".formatted(message.getFrom().getUserName()), message.getChatId());

        }
    }

    //TODO track users join/leave
    //TODO annotate number of members when started?
    @SneakyThrows
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
        Optional<GameSession> gameSession = gameSessionService.recordGameSession(message.getFrom().getId(), message.getChatId(), message.getFrom().getUserName(),
                                                                                 gameDuration.get());

        gameSession.ifPresent(session -> messageService.info(SUBMISSION_MESSAGE_TEMPLATE.formatted(session.getUser().getUserName(),
                                                                                                   session.getGame().name().toLowerCase(),
                                                                                                   FormatUtils.formatDuration(session.getDuration())),
                                                             message.getChat().getId()));

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
    public void deleteTodayRecord(final Message message, final String[] arguments) {
        if (arguments == null || arguments.length == 0) {
            messageService.error("Please provide a game name. Example: /delete queens", message.getChatId());
            return;
        }

        String gameName = arguments[0];
        GameType gameType;
        try {
            gameType = GameType.valueOf(gameName.toUpperCase());
        } catch (IllegalArgumentException e) {
            messageService.error("'%s' is not a valid game.".formatted(gameName), message.getChatId());
            return;
        }
        gameSessionService.deleteTodaySession(message.getFrom().getId(), message.getChatId(), gameType);
        messageService.info("Your result for *%s* has been deleted.".formatted(gameName.toUpperCase()), message.getChatId());
    }

    @Transactional
    public void deleteTodayRecords(final Message message) {
        gameSessionService.deleteTodaySessions(message.getFrom().getId(), message.getChatId());
        messageService.success("All your records for today have been deleted.", message.getChatId());
    }

    public void dailyRanking(final Message message) {
        telegramGroupService.findGroup(message.getChat().getId())
                            .ifPresent(groupRankingService::createDailyRanking);
    }

    public void listTrackedGames(final Message message) {
        Long chatId = message.getChatId();
        try {
            Set<GameType> trackedGames = telegramGroupService.listTrackedGames(chatId);
            if (trackedGames == null || trackedGames.isEmpty()) {
                messageService.error("This group is not tracking any games.", chatId);
            } else {
                String text = trackedGames.stream()
                                          .sorted()
                                          .map(game -> "%s %s".formatted(FormatUtils.gameIcon(game), game.name()))
                                          .collect(Collectors.joining("\n"));

                messageService.info("This group is currently tracking:\n" + text, chatId);
            }
        } catch (GroupNotFoundException e) {
            messageService.error("Group not registered. Must execute /start command first", message.getChatId());
        }
    }
}
