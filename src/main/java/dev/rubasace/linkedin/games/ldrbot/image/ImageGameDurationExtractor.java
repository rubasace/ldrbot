package dev.rubasace.linkedin.games.ldrbot.image;

import dev.rubasace.linkedin.games.ldrbot.metrics.MetricsConstants;
import dev.rubasace.linkedin.games.ldrbot.session.GameDuration;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Duration;
import java.util.Optional;

@Component
public class ImageGameDurationExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageGameDurationExtractor.class);

    private final ImageGameExtractor imageGameExtractor;
    private final ImageDurationExtractor imageDurationExtractor;
    private final MeterRegistry meterRegistry;
    private final Counter ocrAttemptsCounter;
    private final Counter ocrErrorsCounter;

    ImageGameDurationExtractor(final ImageGameExtractor imageGameExtractor,
                               final ImageDurationExtractor imageDurationExtractor,
                               final MeterRegistry meterRegistry) {
        this.imageGameExtractor = imageGameExtractor;
        this.imageDurationExtractor = imageDurationExtractor;
        this.meterRegistry = meterRegistry;
        this.ocrAttemptsCounter = meterRegistry.counter(MetricsConstants.OCR_ATTEMPTS);
        this.ocrErrorsCounter = meterRegistry.counter(MetricsConstants.OCR_ERRORS);
    }

    public Optional<GameDuration> extractGameDuration(final File imageFile, final Long chatId, final UserInfo userInfo) throws GameDurationExtractionException {
        try (Mat image = opencv_imgcodecs.imread(imageFile.getAbsolutePath())) {
            Optional<GameType> gameType = imageGameExtractor.extractGame(image);
            if (gameType.isEmpty()) {
                return Optional.empty();
            }
            ocrAttemptsCounter.increment();
            try {
                Duration duration = imageDurationExtractor.extractDuration(image, gameType.get().getColors());
                return Optional.of(new GameDuration(gameType.get(), duration));
            } catch (DurationOCRException e) {
                ocrErrorsCounter.increment();
                if (e.getCause() != null) {
                    LOGGER.error(e.getMessage(), e);
                } else {
                    LOGGER.error(e.getMessage());
                }
                throw new GameDurationExtractionException(chatId, userInfo, gameType.get());
            }
        }
    }
}
