package dev.rubasace.linkedin.games_tracker.session;

public enum GameType {
    QUEENS("#7C569F"),
    TANGO("#38495B"),
    ZIP("#EE5C14");

    private final String color;

    GameType(final String color) {
        this.color = color;
    }

    public String getColor() {
        return color;
    }
}
