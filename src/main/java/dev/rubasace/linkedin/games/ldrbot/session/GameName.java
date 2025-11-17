package dev.rubasace.linkedin.games.ldrbot.session;

import lombok.Getter;

@Getter
public enum GameName {
    QUEENS("Queens", GameType.QUEENS),
    TANGO("Tango", GameType.TANGO),
    CROSSCLIMB("Crossclimb", GameType.CROSSCLIMB),
    SUDOKU("Mini Sudoku", GameType.SUDOKU),
    ZIP("Zip", GameType.ZIP);

    private final String name;

    private final GameType type;

    GameName(final String name, final GameType type) {
        this.name = name;
        this.type = type;
    }

}
