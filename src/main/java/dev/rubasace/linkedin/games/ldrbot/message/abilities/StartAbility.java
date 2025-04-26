package dev.rubasace.linkedin.games.ldrbot.message.abilities;

import dev.rubasace.linkedin.games.ldrbot.chat.ChatConstants;
import dev.rubasace.linkedin.games.ldrbot.chat.CustomTelegramClient;
import dev.rubasace.linkedin.games.ldrbot.message.AbilityImplementation;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.abilitybots.api.objects.Ability;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import static org.telegram.telegrambots.abilitybots.api.objects.Locality.ALL;
import static org.telegram.telegrambots.abilitybots.api.objects.Privacy.PUBLIC;

@Component
class StartAbility implements AbilityImplementation {

    private static final String PRIVATE_START_MESSAGE = """
            👋 Hello! I'm LDRBot — your daily Linkedin puzzle leaderboard assistant.
            
            To get started, add me to a Telegram group. I’ll track your group’s results for LinkedIn games like Queens, Tango, and Zip, and build a daily ranking automatically.
            
            For the moment I don't support private chat features, but I'm working on it!
            
            
            """ + ChatConstants.HELP_SUGGESTION;

    private static final String GROUP_START_MESSAGE = """
            👀 I’ve already got my eye on this group!
            
            Ready to record those times and crown the daily champs 🏆
            
            """ + ChatConstants.HELP_SUGGESTION;

    private final CustomTelegramClient customTelegramClient;

    StartAbility(final CustomTelegramClient customTelegramClient) {
        this.customTelegramClient = customTelegramClient;
    }


    @Override
    public Ability getAbility() {
        return Ability.builder()
                      .name("start")
                      .info("Start interacting with LDRBot. Required for private messages.")
                      .locality(ALL)
                      .privacy(PUBLIC)
                      .action(ctx -> start(ctx.update().getMessage()))
                      .build();
    }

    private void start(final Message message) {
        if (message.getChat().isGroupChat()) {
            customTelegramClient.message(GROUP_START_MESSAGE, message.getChatId());
        } else {
            customTelegramClient.message(PRIVATE_START_MESSAGE, message.getChatId());
        }
    }

}
