package dev.rubasace.linkedin.games.ldrbot.message.abilities;

import dev.rubasace.linkedin.games.ldrbot.chat.CustomTelegramClient;
import dev.rubasace.linkedin.games.ldrbot.util.UsageFormatUtils;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.abilitybots.api.objects.Ability;
import org.telegram.telegrambots.abilitybots.api.util.AbilityExtension;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.telegram.telegrambots.abilitybots.api.objects.Locality.ALL;
import static org.telegram.telegrambots.abilitybots.api.objects.Privacy.PUBLIC;

@Component
public class HelpAbility implements AbilityExtension, ApplicationListener<ApplicationReadyEvent> {

    private static final String HELP_MESSAGE = """
            🤖 <b>LDRBot Help</b>
            
            Here’s what I can do in this group:
            
            📸 <b>How it works</b>
            
            Send a screenshot of your completed LinkedIn puzzle (Queens, Tango, Zip, or Crossclimb) and I’ll extract your time and track it.
            
            🏆 <b>Daily Competition</b>
            
            Each day, scores are tracked separately per group. I’ll automatically publish the leaderboard once everyone submits, or by the end of the day — alternatively, you can trigger it manually with <code>/ranking</code>.
            
            🛠️ <b>Commands</b>
            
            %s
            
            💡 <b>Tip:</b> I only process screenshots or commands in group messages. Private chat support is coming soon!
            """;

    public static final String COMMAND_HELP_FORMAT = "<b>%s</b> – %s";

    private final ObjectProvider<AbilityExtension> abilityExtensions;
    private final CustomTelegramClient customTelegramClient;
    private String helpMessage;

    HelpAbility(final ObjectProvider<AbilityExtension> abilityExtensions, final CustomTelegramClient customTelegramClient) {
        this.customTelegramClient = customTelegramClient;
        this.abilityExtensions = abilityExtensions;
        this.helpMessage = "Loading...";
    }

    public Ability help() {
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
        Map<String, BotCommand> botCommands = abilityExtensions.stream()
                                                               .flatMap(this::getAbilities)
                                                               .collect(Collectors.toMap(Ability::name, ability -> new BotCommand(ability.name(), ability.info())));
        String commandsSection = this.formatCommands(botCommands);
        this.helpMessage = HELP_MESSAGE.formatted(commandsSection);
    }

    private Stream<Ability> getAbilities(AbilityExtension abilityExtension) {
        return Arrays.stream(abilityExtension.getClass().getMethods())
                     .filter(method -> Ability.class.isAssignableFrom(method.getReturnType()))
                     .map(method -> getInvoke(abilityExtension, method));
    }

    @SneakyThrows
    private static Ability getInvoke(final AbilityExtension abilityExtension, final Method method) {
        return (Ability) method.invoke(abilityExtension);
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
                .map(u -> (COMMAND_HELP_FORMAT + "\n    usage:  <code>%s</code>").formatted(commandName, UsageFormatUtils.extractDescription(description), u))
                .orElse(COMMAND_HELP_FORMAT.formatted(commandName, description));
    }
}
