package dev.rubasace.linkedin.games.ldrbot.util;

public class EscapeUtils {

    public static String escapeText(final String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
