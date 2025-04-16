package dev.rubasace.linkedin.games_tracker.game;

import dev.rubasace.linkedin.games_tracker.image.ImageHelper;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;

@Component
public class GameDetector {

    private final ImageHelper imageHelper;

    public GameDetector(final ImageHelper imageHelper) {
        this.imageHelper = imageHelper;
    }

    public Optional<GameType> detectGame(final File imageFile) {
        try (Mat image = opencv_imgcodecs.imread(imageFile.getAbsolutePath())) {
            return Arrays.stream(GameType.values())
                         .filter(gameType -> imageHelper.isColorDominant(image, gameType.getColor()))
                         .findFirst();
        }
    }
}