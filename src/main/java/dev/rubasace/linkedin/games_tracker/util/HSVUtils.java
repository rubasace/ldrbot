package dev.rubasace.linkedin.games_tracker.util;

import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;

import static org.bytedeco.opencv.global.opencv_core.CV_8UC3;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2HSV;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;

public class HSVUtils {

    public static Scalar[] getHSVRangeFromHex(final String hex, final int hTolerance, final int sTolerance, final int vTolerance) {
        int r = Integer.parseInt(hex.substring(1, 3), 16);
        int g = Integer.parseInt(hex.substring(3, 5), 16);
        int b = Integer.parseInt(hex.substring(5, 7), 16);

        // Convert BGR to HSV
        Mat bgr = new Mat(1, 1, CV_8UC3, new Scalar(b, g, r, 0));
        Mat hsv = new Mat();
        cvtColor(bgr, hsv, COLOR_BGR2HSV);

        UByteIndexer idx = hsv.createIndexer();
        int h = idx.get(0, 0, 0);
        int s = idx.get(0, 0, 1);
        int v = idx.get(0, 0, 2);

        Scalar lower = new Scalar(Math.max(h - hTolerance, 0), Math.max(s - sTolerance, 0), Math.max(v - vTolerance, 0), 0);
        Scalar upper = new Scalar(Math.min(h + hTolerance, 179), Math.min(s + sTolerance, 255), Math.min(v + vTolerance, 255), 0);

        return new Scalar[]{lower, upper};
    }
}
