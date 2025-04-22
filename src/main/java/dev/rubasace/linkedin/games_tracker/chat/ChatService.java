package dev.rubasace.linkedin.games_tracker.chat;

import dev.rubasace.linkedin.games_tracker.group.GroupNotFoundException;
import dev.rubasace.linkedin.games_tracker.group.TelegramGroupService;
import dev.rubasace.linkedin.games_tracker.message.InvalidUserInputException;
import dev.rubasace.linkedin.games_tracker.session.GameType;
import dev.rubasace.linkedin.games_tracker.util.FormatUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.telegram.telegrambots.abilitybots.api.objects.Ability;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final String HELP_MESSAGE = """
            🤖 <b>LinkedIn Games Tracker Help</b>
            
            Here’s what I can do in this group:
            
            📸 <b>How it works</b>
            
            Send a screenshot of your completed LinkedIn puzzle (Queens, Tango, Zip) and I’ll extract your time and track it.
            
            🏆 <b>Daily Competition</b>
            
            Each day, scores are tracked separately per group. I’ll automatically publish the leaderboard once everyone submits, or by the end of the day — alternatively, you can trigger it manually with <code>/daily</code>.
            
            🛠️ <b>Commands</b>
            
            %s
            
            💡 <b>Tip:</b> I only process screenshots or commands in group messages. Private chat support is coming soon!
            """;
    private static final String PRIVATE_START_MESSAGE = """
            👋 Hello! I'm the LinkedIn Games Tracker bot.
            
            To get started, add me to a Telegram group. I’ll track puzzle results for games like Queens, Tango, and Zip and keep a daily leaderboard.
            
            Use /help to see what I can do.
            """;

    private final CustomTelegramClient customTelegramClient;
    private final TelegramGroupService telegramGroupService;

    ChatService(final CustomTelegramClient customTelegramClient, final TelegramGroupService telegramGroupService) {
        this.customTelegramClient = customTelegramClient;
        this.telegramGroupService = telegramGroupService;
    }

    public void listTrackedGames(final Long chatId) throws GroupNotFoundException, InvalidUserInputException {
        Set<GameType> trackedGames = telegramGroupService.listTrackedGames(chatId);
        if (CollectionUtils.isEmpty(trackedGames)) {
            throw new InvalidUserInputException("This group is not tracking any games.", chatId);
        } else {
            String text = trackedGames.stream()
                                      .sorted()
                                      .map(game -> "%s %s".formatted(FormatUtils.gameIcon(game), game.name()))
                                      .collect(Collectors.joining("\n"));

            customTelegramClient.info("This group is currently tracking:\n" + text, chatId);
        }
    }


    public void help(final Long chatId, final Map<String, Ability> abilities) {
        String commandsSection = this.formatAbilities(abilities);
        customTelegramClient.html(HELP_MESSAGE.formatted(commandsSection), chatId);
    }

    private String formatAbilities(final Map<String, Ability> abilities) {
        return abilities.values().stream()
                        .filter(ability -> ability.info() != null)
                        .sorted(Comparator.comparing(Ability::name))
                        .map(ability -> "/%s - %s".formatted(ability.name(), escapeInfo(ability)))
                        .collect(Collectors.joining("\n"));
    }

    private String escapeInfo(final Ability ability) {
        return ability.info()
                      .replace("&", "&amp;")
                      .replace("<", "&lt;")
                      .replace(">", "&gt;");
    }

    public void privateStart(final Long chatId) {
        customTelegramClient.html(PRIVATE_START_MESSAGE, chatId);
    }
}
