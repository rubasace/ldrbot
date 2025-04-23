package dev.rubasace.linkedin.games.ldrbot.image;

import lombok.Getter;

@Getter
public class DurationOCRException extends Exception {

    private final String message;

    public DurationOCRException(final String message) {
        this.message = message;
    }

    public DurationOCRException(final Throwable cause) {
        super(cause);
        this.message = cause.getMessage();
    }
}
