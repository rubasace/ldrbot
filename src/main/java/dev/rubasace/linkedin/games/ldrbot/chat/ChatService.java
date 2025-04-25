package dev.rubasace.linkedin.games.ldrbot.chat;

import dev.rubasace.linkedin.games.ldrbot.group.GroupInfo;
import dev.rubasace.linkedin.games.ldrbot.group.GroupNotFoundException;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupService;
import dev.rubasace.linkedin.games.ldrbot.message.InvalidUserInputException;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.util.EscapeUtils;
import dev.rubasace.linkedin.games.ldrbot.util.FormatUtils;
import dev.rubasace.linkedin.games.ldrbot.util.UsageFormatUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final String HELP_MESSAGE = """
            🤖 <b>LDRBot Help</b>
            
            Here’s what I can do in this group:
            
            📸 <b>How it works</b>
            
            Send a screenshot of your completed LinkedIn puzzle (Queens, Tango, Zip) and I’ll extract your time and track it.
            
            🏆 <b>Daily Competition</b>
            
            Each day, scores are tracked separately per group. I’ll automatically publish the leaderboard once everyone submits, or by the end of the day — alternatively, you can trigger it manually with <code>/ranking</code>.
            
            🛠️ <b>Commands</b>
            
            %s
            
            💡 <b>Tip:</b> I only process screenshots or commands in group messages. Private chat support is coming soon!
            """;
    private static final String PRIVATE_START_MESSAGE = """
            👋 Hello! I'm LDRBot — your daily Linkedin puzzle leaderboard assistant.
            
            To get started, add me to a Telegram group. I’ll track your group’s results for LinkedIn games like Queens, Tango, and Zip, and build a daily ranking automatically.
            
            For the moment I don't support private chat features, but I'm working on it!
            
            
            """ + ChatConstants.HELP_SUGGESTION;

    private static final String GROUP_START_MESSAGE = """
            👀 I’ve already got my eye on this group!
            
            Ready to record those times and crown the daily champs 🏆
            
            """ + ChatConstants.HELP_SUGGESTION;
    public static final String COMMAND_HELP_FORMAT = "<b>%s</b> – %s";

    private final CustomTelegramClient customTelegramClient;
    private final TelegramGroupService telegramGroupService;

    ChatService(final CustomTelegramClient customTelegramClient, final TelegramGroupService telegramGroupService) {
        this.customTelegramClient = customTelegramClient;
        this.telegramGroupService = telegramGroupService;
    }

    public void listTrackedGames(final GroupInfo groupInfo) throws GroupNotFoundException, InvalidUserInputException {
        Set<GameType> trackedGames = telegramGroupService.listTrackedGames(groupInfo);
        if (CollectionUtils.isEmpty(trackedGames)) {
            throw new InvalidUserInputException("This group is not tracking any games.", groupInfo.chatId());
        } else {
            String text = trackedGames.stream()
                                      .sorted()
                                      .map(game -> "%s %s".formatted(FormatUtils.gameIcon(game), game.name()))
                                      .collect(Collectors.joining("\n"));

            customTelegramClient.info("This group is currently tracking:\n" + text, groupInfo.chatId());
        }
    }


    public void help(final Long chatId, final Map<String, BotCommand> botCommands) {
        String commandsSection = this.formatCommands(botCommands);
        customTelegramClient.html(HELP_MESSAGE.formatted(commandsSection), chatId);
    }

    private String formatCommands(final Map<String, BotCommand> botCommands) {
        return botCommands.values().stream()
                          .sorted(Comparator.comparing(BotCommand::getCommand))
                          .map(this::formatCommandLine)
                          .collect(Collectors.joining("\n"));
    }

    @NotNull
    private String formatCommandLine(final BotCommand command) {
        String commandName = "/" + command.getCommand();
        String description = command.getDescription();
        Optional<String> usage = UsageFormatUtils.extractUsage(description);

        return usage
                .map(u -> (COMMAND_HELP_FORMAT + "\n    usage:  <code>%s</code>").formatted(commandName, EscapeUtils.escapeText(UsageFormatUtils.extractDescription(description)),
                                                                                            EscapeUtils.escapeText((u))))
                .orElse(COMMAND_HELP_FORMAT.formatted(commandName, EscapeUtils.escapeText(description)));
    }

    public void privateStart(final Long chatId) {
        customTelegramClient.html(PRIVATE_START_MESSAGE, chatId);
    }

    public void groupStart(final Long chatId) {
        customTelegramClient.html(GROUP_START_MESSAGE, chatId);
    }


}
