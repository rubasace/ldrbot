package dev.rubasace.linkedin.games_tracker.session;


import lombok.Getter;

@Getter
public class AlreadyRegisteredSession extends Exception {

    private final String username;
    private final GameType game;


    public AlreadyRegisteredSession(final String username, final GameType game) {
        this.username = username;
        this.game = game;
    }

}
