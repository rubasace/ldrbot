package dev.rubasace.linkedin.games_tracker.image;

import dev.rubasace.linkedin.games_tracker.session.GameDuration;
import dev.rubasace.linkedin.games_tracker.session.GameType;
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

    ImageGameDurationExtractor(final ImageGameExtractor imageGameExtractor, final ImageDurationExtractor imageDurationExtractor) {
        this.imageGameExtractor = imageGameExtractor;
        this.imageDurationExtractor = imageDurationExtractor;
    }

    public Optional<GameDuration> extractGameDuration(final File imageFile, final Long chatId, final String username) throws GameDurationExtractionException {
        try (Mat image = opencv_imgcodecs.imread(imageFile.getAbsolutePath())) {
            Optional<GameType> gameType = imageGameExtractor.extractGame(image);
            if (gameType.isEmpty()) {
                return Optional.empty();
            }
            try {
                Duration duration = imageDurationExtractor.extractDuration(image);
                return Optional.of(new GameDuration(gameType.get(), duration));
            } catch (DurationOCRException e) {
                if (e.getCause() != null) {
                    LOGGER.error(e.getMessage(), e);
                } else {
                    LOGGER.error(e.getMessage());
                }
                throw new GameDurationExtractionException(chatId, username, gameType.get());
            }
        }
    }
}