package dev.rubasace.linkedin.games.ldrbot.util;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KeyboardMarkupUtils {

    // Standard cancel/exit button used across guided flows
    public static final ButtonData CANCEL_BUTTON = ButtonData.of("cancel", "❌ Cancel");

    public static InlineKeyboardMarkup createTwoColumnLayout(final String actionPrefix, final ButtonData... buttonData) {
        return createLayout(actionPrefix, 2, buttonData);
    }

    public static InlineKeyboardMarkup createTwoColumnLayoutWithCancel(final String actionPrefix, final ButtonData... buttonData) {
        // Build the base layout first
        InlineKeyboardMarkup base = createTwoColumnLayout(actionPrefix, buttonData);
        // Copy existing rows and append a new row containing only the cancel button
        List<InlineKeyboardRow> rows = new ArrayList<>(base.getKeyboard());
        InlineKeyboardRow cancelRow = new InlineKeyboardRow();
        cancelRow.add(createButton(CANCEL_BUTTON, actionPrefix));
        rows.add(cancelRow);
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public static InlineKeyboardMarkup createLayout(final String actionPrefix, final int columns, final ButtonData... buttonData) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow currentRow = new InlineKeyboardRow();
        rows.add(currentRow);

        for (int i = 0; i < buttonData.length; i++) {
            ButtonData button = buttonData[i];
            if (i > 0 && i % columns == 0) {
                currentRow = new InlineKeyboardRow();
                rows.add(currentRow);
            }
            currentRow.add(createButton(button, actionPrefix));
        }
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public static InlineKeyboardMarkup createLayoutWithCancel(final String actionPrefix, final int columns, final ButtonData... buttonData) {
        // Build the base layout first
        InlineKeyboardMarkup base = createLayout(actionPrefix, columns, buttonData);
        // Copy existing rows and append a new row containing only the cancel button
        List<InlineKeyboardRow> rows = new ArrayList<>(base.getKeyboard());
        InlineKeyboardRow cancelRow = new InlineKeyboardRow();
        cancelRow.add(createButton(CANCEL_BUTTON, actionPrefix));
        rows.add(cancelRow);
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
