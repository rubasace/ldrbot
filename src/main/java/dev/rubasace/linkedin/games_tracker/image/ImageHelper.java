package dev.rubasace.linkedin.games_tracker.image;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.jetbrains.annotations.NotNull;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class ImageHelper {
    private static final int H_TOLERANCE = 10;
    private static final int S_TOLERANCE = 60;
    private static final int V_TOLERANCE = 60;

    ImageHelper() {
        Loader.load(opencv_core.class);
    }

    boolean isColorPresent(Mat image, String color, double percentage) {
        Mat mask = getColorMask(image, color);

        double count = opencv_core.countNonZero(mask);
        double totalPixels = image.rows() * image.cols();

        double colorRatio = (count / totalPixels);
        return colorRatio >= percentage;
    }

    @NotNull
    private Mat getColorMask(final Mat image, final String color) {
        Mat hsv = new Mat();
        opencv_imgproc.cvtColor(image, hsv, opencv_imgproc.COLOR_BGR2HSV);

        Scalar[] colorBounds = getHSVRangeFromHex(color);

        Mat lowerBound = new Mat(hsv.size(), hsv.type(), colorBounds[0]);
        Mat upperBound = new Mat(hsv.size(), hsv.type(), colorBounds[1]);
        Mat mask = new Mat();
        opencv_core.inRange(hsv, lowerBound, upperBound, mask);
        return mask;
    }

    private Scalar[] getHSVRangeFromHex(final String hex) {
        int r = Integer.parseInt(hex.substring(1, 3), 16);
        int g = Integer.parseInt(hex.substring(3, 5), 16);
        int b = Integer.parseInt(hex.substring(5, 7), 16);

        // Convert BGR to HSV
        Mat bgr = new Mat(1, 1, opencv_core.CV_8UC3, new Scalar(b, g, r, 0));
        Mat hsv = new Mat();
        opencv_imgproc.cvtColor(bgr, hsv, opencv_imgproc.COLOR_BGR2HSV);

        UByteIndexer idx = hsv.createIndexer();
        int h = idx.get(0, 0, 0);
        int s = idx.get(0, 0, 1);
        int v = idx.get(0, 0, 2);

        Scalar lower = new Scalar(Math.max(h - H_TOLERANCE, 0), Math.max(s - S_TOLERANCE, 0), Math.max(v - V_TOLERANCE, 0), 0);
        Scalar upper = new Scalar(Math.min(h + H_TOLERANCE, 179), Math.min(s + S_TOLERANCE, 255), Math.min(v + V_TOLERANCE, 255), 0);

        return new Scalar[]{lower, upper};
    }

    Optional<Rect> findLargestRegionOfColor(Mat image, String color) {
        Mat mask = getColorMask(image, color);

        MatVector contours = new MatVector();
        opencv_imgproc.findContours(mask.clone(), contours, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        if (contours.size() == 0) {
            return Optional.empty();
        }

        double maxArea = 0;
        Rect maxRect = null;

        for (int i = 0; i < contours.size(); i++) {
            double area = opencv_imgproc.contourArea(contours.get(i));
            if (area > maxArea) {
                maxArea = area;
                maxRect = opencv_imgproc.boundingRect(contours.get(i));
            }
        }

        return Optional.ofNullable(maxRect);
    }

}