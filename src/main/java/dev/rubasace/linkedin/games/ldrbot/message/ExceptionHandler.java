package dev.rubasace.linkedin.games.ldrbot.message;

import dev.rubasace.linkedin.games.ldrbot.chat.ChatConstants;
import dev.rubasace.linkedin.games.ldrbot.chat.CustomTelegramClient;
import dev.rubasace.linkedin.games.ldrbot.chat.UserFeedbackException;
import dev.rubasace.linkedin.games.ldrbot.image.GameDurationExtractionException;
import dev.rubasace.linkedin.games.ldrbot.session.GameNameNotFoundException;
import dev.rubasace.linkedin.games.ldrbot.session.SessionAlreadyRegisteredException;
import dev.rubasace.linkedin.games.ldrbot.user.UserNotFoundException;
import dev.rubasace.linkedin.games.ldrbot.util.FormatUtils;
import org.springframework.stereotype.Component;

@Component
class ExceptionHandler {

    private static final String ALREADY_REGISTERED_SESSION_MESSAGE_TEMPLATE = "%s already registered a time for %s. If you need to override the time, please delete the current time through the \"/delete <gameInfo>\" command. In this case: /delete %s. Alternatively, you can delete all your submissions for the day using /deleteall";
    public static final String GAME_DURATION_EXCEPTION_MESSAGE_TEMPLATE = "%s submitted a screenshot for the gameInfo %s, but I couldn’t extract the solving time. This often happens if the image is cropped or covered by overlays like confetti. Try sending a clearer screenshot, or ask an admin to set your time manually using /override %s <time>";
    public static final String USER_NOT_FOUND_EXCEPTION_MESSAGE_TEMPLATE = "User %s not found";

    private static final String UNKNOWN_COMMAND_MESSAGE_TEMPLATE = """
            🤖 Sorry, I don’t recognize the command %s.
            
            
            """ + ChatConstants.HELP_SUGGESTION;
    public static final String GAME_NOT_FOUND_EXCEPTION_MESSAGE = "'%s' is not a valid game name.";

    private final CustomTelegramClient customTelegramClient;

    ExceptionHandler(final CustomTelegramClient customTelegramClient) {
        this.customTelegramClient = customTelegramClient;
    }

    void notifyUserFeedbackException(final UserFeedbackException userFeedbackException) {
        if (userFeedbackException instanceof UnknownCommandException unknownCommandException) {
            customTelegramClient.sendErrorMessage(UNKNOWN_COMMAND_MESSAGE_TEMPLATE.formatted(unknownCommandException.getCommand()), unknownCommandException.getChatId());
        } else if (userFeedbackException instanceof SessionAlreadyRegisteredException sessionAlreadyRegisteredException) {
            String text = ALREADY_REGISTERED_SESSION_MESSAGE_TEMPLATE.formatted(FormatUtils.formatUserMention(sessionAlreadyRegisteredException.getUserInfo()),
                                                                                sessionAlreadyRegisteredException.getGameInfo().name(),
                                                                                sessionAlreadyRegisteredException.getGameInfo().name());
            customTelegramClient.sendErrorMessage(text, sessionAlreadyRegisteredException.getChatId());
        } else if (userFeedbackException instanceof UserNotFoundException userNotFoundException) {
            customTelegramClient.sendErrorMessage(USER_NOT_FOUND_EXCEPTION_MESSAGE_TEMPLATE.formatted(FormatUtils.formatUserMention(userNotFoundException.getUserInfo())),
                                                  userNotFoundException.getChatId());
        } else if (userFeedbackException instanceof GameNameNotFoundException gameNameNotFoundException) {
            customTelegramClient.sendErrorMessage(GAME_NOT_FOUND_EXCEPTION_MESSAGE.formatted(gameNameNotFoundException.getGameName()), gameNameNotFoundException.getChatId());
        } else if (userFeedbackException instanceof GameDurationExtractionException gameDurationExtractionException) {
            String text = GAME_DURATION_EXCEPTION_MESSAGE_TEMPLATE.formatted(FormatUtils.formatUserMention(gameDurationExtractionException.getUserInfo()),
                                                                             gameDurationExtractionException.getGameType().name(),
                                                                             gameDurationExtractionException.getGameType().name().toLowerCase());
            customTelegramClient.sendErrorMessage(text, gameDurationExtractionException.getChatId());
        } else if (userFeedbackException instanceof InvalidUserInputException invalidUserInputException) {
            customTelegramClient.sendErrorMessage(invalidUserInputException.getMessage(), invalidUserInputException.getChatId());
        }
    }
}
