package dev.rubasace.linkedin.games.ldrbot.session;

import lombok.Getter;

@Getter
public enum GameType {
    QUEENS("#7C569F"),
    TANGO("#38495B", "#25394E"),
    CROSSCLIMB("#057B8B"),
    SUDOKU("#399767", "#42B96D", "#46C674"),
    ZIP("#EE5C14");

    private final String[] colors;

    GameType(final String... colors) {
        this.colors = colors;
    }

}
