package dev.rubasace.linkedin.games.ldrbot.metrics;

public final class MetricsConstants {

    private MetricsConstants() {
    }

    // Message processing
    public static final String MESSAGES_PROCESSED = "ldrbot.messages.processed";
    public static final String MESSAGES_LATENCY = "ldrbot.messages.latency";
    public static final String MESSAGES_INFLIGHT = "ldrbot.messages.inflight";

    // OCR
    public static final String OCR_ATTEMPTS = "ldrbot.ocr.attempts";
    public static final String OCR_ERRORS = "ldrbot.ocr.errors";

    // Errors
    public static final String ERRORS = "ldrbot.errors";
    public static final String ERRORS_UNEXPECTED = "ldrbot.errors.unexpected";

    // Background tasks
    public static final String BACKGROUND_DURATION = "ldrbot.background.duration";
    public static final String BACKGROUND_ERRORS = "ldrbot.background.errors";

    // Game sessions
    public static final String SESSIONS_REGISTERED = "ldrbot.sessions.registered";
    public static final String SESSIONS_DELETED = "ldrbot.sessions.deleted";

    // Business gauges
    public static final String GROUPS_ACTIVE = "ldrbot.groups.active";
    public static final String USERS_TOTAL = "ldrbot.users.total";

    // Tags
    public static final String TAG_ERROR_TYPE = "error_type";
    public static final String TAG_TASK_NAME = "task_name";
    public static final String TAG_GAME_TYPE = "game_type";
}
