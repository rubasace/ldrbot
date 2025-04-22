package dev.rubasace.linkedin.games_tracker.exception;

import dev.rubasace.linkedin.games_tracker.chat.NotificationService;
import dev.rubasace.linkedin.games_tracker.chat.UserFeedbackException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
class BotExceptionHandlerAspect {
    private static final Logger LOGGER = LoggerFactory.getLogger(BotExceptionHandlerAspect.class);

    private final NotificationService notificationService;

    public BotExceptionHandlerAspect(final NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Around("@within(dev.rubasace.linkedin.games_tracker.exception.HandleBotExceptions)")
    public Object handleBotExceptions(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (UserFeedbackException e) {
            notificationService.notifyUserFeedbackException(e);
        } catch (Exception e) {
            LOGGER.error("An unexpected error occurred", e);
        }
        return null;
    }

}