package dev.rubasace.linkedin.games.ldrbot.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageEscapeHelperTest {

    private final MessageEscapeHelper messageEscapeHelper = new MessageEscapeHelper();

    @Test
    void shouldEscapePlainText() {
        String input = "Hello & welcome to <LDRBot>!";
        String expected = "Hello &amp; welcome to &lt;LDRBot&gt;!";

        assertEquals(expected, messageEscapeHelper.escapeMessage(input));
    }

    @Test
    void shouldNotEscapeAllowedSimpleTags() {
        String input = "Hello <b>World</b>!";
        String expected = "Hello <b>World</b>!";

        assertEquals(expected, messageEscapeHelper.escapeMessage(input));
    }

    @Test
    void shouldNotEscapeAllowedTagsWithAttributes() {
        String input = "Click <a href=\"https://example.com\">here</a>!";
        String expected = "Click <a href=\"https://example.com\">here</a>!";

        assertEquals(expected, messageEscapeHelper.escapeMessage(input));
    }

    @Test
    void shouldEscapeUnknownTags() {
        String input = "Text <unknown>bad</unknown> text";
        String expected = "Text &lt;unknown&gt;bad&lt;/unknown&gt; text";
        assertEquals(expected, messageEscapeHelper.escapeMessage(input));
    }

    @Test
    void shouldEscapeUnclosedTags() {
        String input = "Text with <openTag";
        String expected = "Text with &lt;openTag";
        assertEquals(expected, messageEscapeHelper.escapeMessage(input));
    }

    @Test
    void shouldHandleEmptyString() {
        String input = "";
        String expected = "";
        assertEquals(expected, messageEscapeHelper.escapeMessage(input));
    }

    @Test
    void shouldHandleOnlySpecialCharacters() {
        String input = "&<>";
        String expected = "&amp;&lt;&gt;";
        assertEquals(expected, messageEscapeHelper.escapeMessage(input));
    }

}