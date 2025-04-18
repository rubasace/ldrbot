package dev.rubasace.linkedin.games_tracker.session;


import lombok.Getter;

@Getter
public class UnrecognizedGameException extends Exception {

    private final String gameName;

    public UnrecognizedGameException(final String gameName) {
        this.gameName = gameName;
    }

}
