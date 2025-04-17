package dev.rubasace.linkedin.games_tracker.image;

import dev.rubasace.linkedin.games_tracker.game.GameDuration;
import dev.rubasace.linkedin.games_tracker.game.GameType;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Optional;

@Component
public class ImageGameDurationExtractor {

    private final ImageGameExtractor imageGameExtractor;
    private final ImageDurationExtractor imageDurationExtractor;

    public ImageGameDurationExtractor(final ImageGameExtractor imageGameExtractor, final ImageDurationExtractor imageDurationExtractor) {
        this.imageGameExtractor = imageGameExtractor;
        this.imageDurationExtractor = imageDurationExtractor;
    }

    public Optional<GameDuration> extractGameDuration(final File imageFile) {
        try (Mat image = opencv_imgcodecs.imread(imageFile.getAbsolutePath())) {
            Optional<GameType> gameType = imageGameExtractor.extractGame(image);
            return gameType.flatMap(type -> imageDurationExtractor.extractDuration(image)
                                                                  .map(duration -> new GameDuration(type, duration)));

        }
    }
}