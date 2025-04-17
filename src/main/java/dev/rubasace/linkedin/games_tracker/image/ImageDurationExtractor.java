package dev.rubasace.linkedin.games_tracker.image;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;

@Component
class ImageDurationExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageDurationExtractor.class);
    private static final Pattern TIMER_PATTERN = Pattern.compile("\\b(\\d{1,2}):?(\\d{2})\\b");

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
            imwrite(temp.getAbsolutePath(), cropped);
            String text = imageTextExtractor.extractText(temp);
            Matcher matcher = TIMER_PATTERN.matcher(text);

            if (matcher.find()) {
                int minutes = Integer.parseInt(matcher.group(1));
                int seconds = Integer.parseInt(matcher.group(2));
                return Optional.of(Duration.ofMinutes(minutes).plusSeconds(seconds));
            } else {
                LOGGER.warn("No timer found in OCR result");
                return Optional.empty();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}