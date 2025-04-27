package dev.rubasace.linkedin.games.ldrbot.message.config;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;

class KeyboardMarkupUtils {

    static InlineKeyboardMarkup createTwoColumnLayout(final String actionPrefix, final ConfigAction... configActions) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow currentRow = new InlineKeyboardRow();
        rows.add(currentRow);

        for (int i = 0; i < configActions.length; i++) {
            ConfigAction configAction = configActions[i];
            if (i > 0 && i % 2 == 0) {
                currentRow = new InlineKeyboardRow();
                rows.add(currentRow);
            }
            currentRow.add(createButton(configAction, actionPrefix));
        }
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private static InlineKeyboardButton createButton(ConfigAction configAction, String actionPrefix) {
        return createButton(configAction.getTitle(), configAction.getKey(), actionPrefix);
    }

    private static InlineKeyboardButton createButton(String title, String action, String actionPrefix) {
        return InlineKeyboardButton.builder().text(title).callbackData(actionPrefix + action).build();
    }

}
