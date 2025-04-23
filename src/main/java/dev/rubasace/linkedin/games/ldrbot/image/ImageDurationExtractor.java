package dev.rubasace.linkedin.games.ldrbot.image;

import dev.rubasace.linkedin.games.ldrbot.util.ParseUtils;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Component
class ImageDurationExtractor {

    private static final String RESULTS_COLOR = "#FDE4A5";

    private final ImageHelper imageHelper;
    private final ImageTextExtractor imageTextExtractor;

    ImageDurationExtractor(final ImageHelper imageHelper, final ImageTextExtractor imageTextExtractor) {
        this.imageHelper = imageHelper;
        this.imageTextExtractor = imageTextExtractor;
    }

    //TODO improve using blockingQueue or something similar
    synchronized Duration extractDuration(final Mat image) throws DurationOCRException {
        Optional<Rect> resultsBox = imageHelper.findLargestRegionOfColor(image, RESULTS_COLOR);
        if (resultsBox.isEmpty()) {
            throw new DurationOCRException("Couldn't find the results area on the image");
        }
        try {
            Mat cropped = new Mat(image, resultsBox.get());
            File temp = File.createTempFile("time-results-section", ".png");
            opencv_imgcodecs.imwrite(temp.getAbsolutePath(), cropped);
            String text = imageTextExtractor.extractText(temp);
            return Arrays.stream(text.split("\n"))
                                                .map(durationText -> ParseUtils.parseDuration(durationText.trim()))
                                                .filter(Optional::isPresent)
                                                .map(Optional::get)
                         .findFirst()
                         .orElseThrow(() -> new DurationOCRException("No timer found in OCR result. Found the following text:\n" + text));

        } catch (IOException e) {
            throw new DurationOCRException(e);
        }
    }
}