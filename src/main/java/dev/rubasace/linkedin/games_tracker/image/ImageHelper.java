package dev.rubasace.linkedin.games_tracker.image;

import dev.rubasace.linkedin.games_tracker.util.HSVUtils;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static org.bytedeco.opencv.global.opencv_core.countNonZero;
import static org.bytedeco.opencv.global.opencv_core.inRange;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2HSV;
import static org.bytedeco.opencv.global.opencv_imgproc.boundingRect;
import static org.bytedeco.opencv.global.opencv_imgproc.contourArea;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.findContours;
import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.RETR_EXTERNAL;

@Component
class ImageHelper {
    private static final int H_TOLERANCE = 10;
    private static final int S_TOLERANCE = 60;
    private static final int V_TOLERANCE = 60;

    private ImageHelper() {
        Loader.load(opencv_core.class);
    }

    boolean isColorPresent(Mat image, String color, double percentage) {
        Mat mask = getColorMask(image, color);

        double count = countNonZero(mask);
        double totalPixels = image.rows() * image.cols();

        double colorRatio = (count / totalPixels);
        return colorRatio >= percentage;
    }

    @NotNull
    private Mat getColorMask(final Mat image, final String color) {
        Mat hsv = new Mat();
        cvtColor(image, hsv, COLOR_BGR2HSV);

        Scalar[] colorBounds = HSVUtils.getHSVRangeFromHex(color, H_TOLERANCE, S_TOLERANCE, V_TOLERANCE);

        Mat lowerBound = new Mat(hsv.size(), hsv.type(), colorBounds[0]);
        Mat upperBound = new Mat(hsv.size(), hsv.type(), colorBounds[1]);
        Mat mask = new Mat();
        inRange(hsv, lowerBound, upperBound, mask);
        return mask;
    }

    Optional<Rect> findLargestRegionOfColor(Mat image, String color) {
        Mat mask = getColorMask(image, color);

        // Find contours
        MatVector contours = new MatVector();
        findContours(mask.clone(), contours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);

        if (contours.size() == 0) {
            return Optional.empty();
        }

        double maxArea = 0;
        Rect maxRect = null;

        for (int i = 0; i < contours.size(); i++) {
            double area = contourArea(contours.get(i));
            if (area > maxArea) {
                maxArea = area;
                maxRect = boundingRect(contours.get(i));
            }
        }

        return Optional.ofNullable(maxRect);
    }

}