package dev.rubasace.linkedin.games.ldrbot.message.abilities;

import dev.rubasace.linkedin.games.ldrbot.chat.CustomTelegramClient;
import dev.rubasace.linkedin.games.ldrbot.message.AbilityImplementation;
import dev.rubasace.linkedin.games.ldrbot.util.EscapeUtils;
import dev.rubasace.linkedin.games.ldrbot.util.UsageFormatUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.abilitybots.api.objects.Ability;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.telegram.telegrambots.abilitybots.api.objects.Locality.ALL;
import static org.telegram.telegrambots.abilitybots.api.objects.Privacy.PUBLIC;

@Component
class HelpAbility implements AbilityImplementation, ApplicationListener<ApplicationReadyEvent> {

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

    public static final String COMMAND_HELP_FORMAT = "<b>%s</b> – %s";

    private final ObjectProvider<AbilityImplementation> abilityImplementations;
    private final CustomTelegramClient customTelegramClient;
    private String helpMessage;

    HelpAbility(final ObjectProvider<AbilityImplementation> abilityImplementations, final CustomTelegramClient customTelegramClient) {
        this.customTelegramClient = customTelegramClient;
        this.abilityImplementations = abilityImplementations;
        this.helpMessage = "Loading...";
    }

    @Override
    public Ability getAbility() {
        return Ability.builder()
                      .name("help")
                      .info("Show available commands and how to use the bot.")
                      .locality(ALL)
                      .privacy(PUBLIC)
                      .action(ctx -> help(ctx.update().getMessage().getChatId()))
                      .build();
    }


    public void help(final Long chatId) {
        customTelegramClient.message(this.helpMessage, chatId);
    }


    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        Map<String, BotCommand> botCommands = abilityImplementations.stream()
                                                                    .map(AbilityImplementation::getAbility)
                                                                    .collect(Collectors.toMap(Ability::name, ability -> new BotCommand(ability.name(), ability.info())));
        String commandsSection = this.formatCommands(botCommands);
        this.helpMessage = HELP_MESSAGE.formatted(commandsSection);
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
}
