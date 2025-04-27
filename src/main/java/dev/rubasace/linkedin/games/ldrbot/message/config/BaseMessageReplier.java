package dev.rubasace.linkedin.games.ldrbot.message.config;

import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Convenience base class to be implemented by classes that want to handle replies, so it takes care of filtering out all messages that aren't relevant by applying prefix filtering
 */
public abstract class BaseMessageReplier {

    private final String id;

    protected BaseMessageReplier(final String id) {
        this.id = id;
    }

    protected boolean shouldHandleReply(final Update update) {
        return update.hasCallbackQuery() && update.getCallbackQuery().getData().startsWith(getPrefix());
    }


    protected String getAction(final Update update) {
        return update.getCallbackQuery().getData().substring(getPrefix().length());
    }

    protected String getPrefix() {
        return id + ":";
    }
}
