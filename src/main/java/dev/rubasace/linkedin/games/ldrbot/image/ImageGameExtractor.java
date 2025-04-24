package dev.rubasace.linkedin.games.ldrbot.image;

import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import org.bytedeco.opencv.opencv_core.Mat;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
class ImageGameExtractor {

    static final double COLOR_PERCENTAGE_THRESHOLD = 0.10;

    private final ImageHelper imageHelper;

    ImageGameExtractor(final ImageHelper imageHelper) {
        this.imageHelper = imageHelper;
    }

    Optional<GameType> extractGame(final Mat image) {
        return Arrays.stream(GameType.values())
                     .filter(gameType -> imageHelper.isColorPresent(image, gameType.getColor(), COLOR_PERCENTAGE_THRESHOLD))
                     .findFirst();
    }
}