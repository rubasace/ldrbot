package dev.rubasace.linkedin.games_tracker.image;

import dev.rubasace.linkedin.games_tracker.util.ParseUtils;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Component
class ImageDurationExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageDurationExtractor.class);

    private static final String RESULTS_COLOR = "#FDE4A5";

    private final ImageHelper imageHelper;
    private final ImageTextExtractor imageTextExtractor;

    ImageDurationExtractor(final ImageHelper imageHelper, final ImageTextExtractor imageTextExtractor) {
        this.imageHelper = imageHelper;
        this.imageTextExtractor = imageTextExtractor;
    }

    Optional<Duration> extractDuration(final Mat image) {
        Optional<Rect> resultsBox = imageHelper.findLargestRegionOfColor(image, RESULTS_COLOR);
        if (resultsBox.isEmpty()) {
            LOGGER.warn("Couldn't find the results area on the image");
            return Optional.empty();
        }
        try {
            Mat cropped = new Mat(image, resultsBox.get());
            File temp = File.createTempFile("time-results-section", ".png");
            opencv_imgcodecs.imwrite(temp.getAbsolutePath(), cropped);
            String text = imageTextExtractor.extractText(temp);
            Optional<Duration> duration = Arrays.stream(text.split("\n"))
                                                .map(durationText -> ParseUtils.parseDuration(durationText.trim()))
                                                .filter(Optional::isPresent)
                                                .map(Optional::get)
                                                .findFirst();
            if (duration.isEmpty()) {
                LOGGER.warn("No timer found in OCR result");
            }
            return duration;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}