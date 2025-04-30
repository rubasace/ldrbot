package dev.rubasace.linkedin.games.ldrbot.message.abilities;

import dev.rubasace.linkedin.games.ldrbot.chat.ChatConstants;
import dev.rubasace.linkedin.games.ldrbot.chat.CustomTelegramClient;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.abilitybots.api.objects.Ability;
import org.telegram.telegrambots.abilitybots.api.util.AbilityExtension;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import static org.telegram.telegrambots.abilitybots.api.objects.Locality.ALL;
import static org.telegram.telegrambots.abilitybots.api.objects.Privacy.PUBLIC;

@Component
public class AboutAbility implements AbilityExtension {

    private static final String ABOUT_MESSAGE = """
            🤖 About LDRBot
            
            LDRBot is an open-source bot that helps Telegram groups track their LinkedIn puzzle scores.
            
            It automatically builds daily rankings based on user-submitted screenshots.
            
            🌐 Project page: <a href="https://github.com/rubasace/ldrbot">GitHub Repository</a>
            
            🏷️ Version: %s
            
            🛠️ Created by <a href="https://github.com/rubasace">@rubasace</a>
            
            📝 Licensed under MIT License.
            
            """ + ChatConstants.HELP_SUGGESTION;

    private final CustomTelegramClient customTelegramClient;
    private final String version;

    AboutAbility(final CustomTelegramClient customTelegramClient) {
        this.customTelegramClient = customTelegramClient;
        Package appPackage = this.getClass().getPackage();
        this.version = (appPackage != null && appPackage.getImplementationVersion() != null)
                ? appPackage.getImplementationVersion()
                : "dev";
    }

    public Ability about() {
        return Ability.builder()
                      .name("about")
                      .info("Learn about LDRBot and its development.")
                      .locality(ALL)
                      .privacy(PUBLIC)
                      .action(ctx -> about(ctx.update().getMessage()))
                      .build();
    }

    private void about(final Message message) {
        customTelegramClient.sendMessage(ABOUT_MESSAGE.formatted(version), message.getChatId());
    }

}
