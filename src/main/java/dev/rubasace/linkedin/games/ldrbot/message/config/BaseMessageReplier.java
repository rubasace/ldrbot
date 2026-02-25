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

    /**
     * Verify that the callback query comes from the expected user.
     * Use this to restrict button interactions to the user who initiated the flow.
     * 
     * @param update The update containing the callback query
     * @param expectedUserId The ID of the user who should be allowed to interact
     * @return true if the callback query is from the expected user, false otherwise
     */
    protected boolean isAuthorizedUser(Update update, Long expectedUserId) {
        if (!update.hasCallbackQuery()) {
            return false;
        }
        Long callbackUserId = update.getCallbackQuery().getFrom().getId();
        return expectedUserId.equals(callbackUserId);
    }
}
