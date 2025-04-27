package dev.rubasace.linkedin.games.ldrbot.session;

import lombok.Getter;

@Getter
public enum GameType {
    QUEENS("#7C569F"),
    TANGO("#38495B"),
    CROSSCLIMB("#057B8B"),
    ZIP("#EE5C14");

    private final String color;

    GameType(final String color) {
        this.color = color;
    }

}
