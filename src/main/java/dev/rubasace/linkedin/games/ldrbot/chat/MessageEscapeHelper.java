package dev.rubasace.linkedin.games.ldrbot.chat;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
class MessageEscapeHelper {

    private static final List<String> ALLOWED_HTML_TAGS = List.of("b",
                                                                  "strong",
                                                                  "i",
                                                                  "em",
                                                                  "u",
                                                                  "ins",
                                                                  "s",
                                                                  "strike",
                                                                  "del",
                                                                  "code",
                                                                  "pre",
                                                                  "a"
    );

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile(
            "</?(" + String.join("|", ALLOWED_HTML_TAGS) + ")(\\s+[^>]*)?>",
            Pattern.CASE_INSENSITIVE
    );

    String escapeMessage(String message) {
        StringBuilder escaped = new StringBuilder();
        Matcher matcher = HTML_TAG_PATTERN.matcher(message);

        int lastEnd = 0;
        while (matcher.find()) {
            // Escape text before the tag
            String before = message.substring(lastEnd, matcher.start());
            escaped.append(escapeHtmlSpecialChars(before));

            // Append the valid tag as-is
            escaped.append(matcher.group());

            lastEnd = matcher.end();
        }
        // Escape anything after the last tag
        String after = message.substring(lastEnd);
        escaped.append(escapeHtmlSpecialChars(after));

        return escaped.toString();
    }

    private String escapeHtmlSpecialChars(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
