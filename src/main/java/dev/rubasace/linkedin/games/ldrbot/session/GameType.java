package dev.rubasace.linkedin.games.ldrbot.session;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum GameType {
    QUEENS("Queens", "#7C569F"),
    TANGO("Tango", "#38495B", "#25394E"),
    CROSSCLIMB("CrossClimb", "#057B8B"),
    SUDOKU("Mini Sudoku", "#399767", "#42B96D", "#46C674"),
    ZIP("Zip", "#EE5C14"),
    PATCHES("Patches", "#FF4848", "#EB4141");

    private final String name;
    private final String[] colors;

    GameType(String name, final String... colors) {
        this.name = name;
        this.colors = colors;
    }

    public static Optional<GameType> fromName(String name) {
        return Arrays.stream(values())
                .filter(gameType -> gameType.getName().equalsIgnoreCase(name.trim()))
                .findFirst();
    }

}
