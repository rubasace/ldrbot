package dev.rubasace.linkedin.games.ldrbot.util;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;

public class KeyboardMarkupUtils {

    public static InlineKeyboardMarkup createTwoColumnLayout(final String actionPrefix, final ButtonData... buttonData) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow currentRow = new InlineKeyboardRow();
        rows.add(currentRow);

        for (int i = 0; i < buttonData.length; i++) {
            ButtonData button = buttonData[i];
            if (i > 0 && i % 2 == 0) {
                currentRow = new InlineKeyboardRow();
                rows.add(currentRow);
            }
            currentRow.add(createButton(button, actionPrefix));
        }
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private static InlineKeyboardButton createButton(ButtonData buttonData, String actionPrefix) {
        return createButton(buttonData.getTitle(), buttonData.getKey(), actionPrefix);
    }

    private static InlineKeyboardButton createButton(String title, String action, String actionPrefix) {
        return InlineKeyboardButton.builder().text(title).callbackData(actionPrefix + action).build();
    }


    public interface ButtonData {

        String getKey();

        String getTitle();

        static ButtonData of(String key, String title) {
            return new ButtonData() {
                @Override
                public String getKey() {
                    return key;
                }

                @Override
                public String getTitle() {
                    return title;
                }
            };
        }

    }


}
