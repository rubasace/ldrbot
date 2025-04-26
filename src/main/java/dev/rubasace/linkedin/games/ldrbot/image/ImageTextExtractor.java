package dev.rubasace.linkedin.games.ldrbot.image;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Function;

@Component
class ImageTextExtractor {

    private static final int MAX_PARALLELISM = 20;
    private static final int PADDING = 20;
    private static final int SCALE_FACTOR = 2;

    private final BlockingQueue<Tesseract> tesseractPool;

    ImageTextExtractor(final TesseractProperties tesseractProperties) {
        System.setProperty("jna.library.path", tesseractProperties.getLibPath());
        this.tesseractPool = new ArrayBlockingQueue<>(MAX_PARALLELISM);
        for (int i = 0; i < MAX_PARALLELISM; i++) {
            tesseractPool.add(createTesseract(tesseractProperties));
        }
    }

    @NotNull
    private Tesseract createTesseract(final TesseractProperties tesseractProperties) {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tesseractProperties.getDataPath());
        tesseract.setLanguage("eng+spa");
        tesseract.setPageSegMode(11);
        tesseract.setVariable("user_defined_dpi", "300");
        return tesseract;
    }

    String extractText(File inputImage) throws IOException {
        Tesseract tesseract = null;
        try {
            tesseract = tesseractPool.take();
            return doExtractText(inputImage, tesseract);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for Tesseract instance", e);
        } finally {
            if (tesseract != null) {
                tesseractPool.offer(tesseract);
            }
        }
    }

    private String doExtractText(final File inputImage, final Tesseract tesseract) throws IOException {
        try {
            File preprocessedImage = preprocessForOCR(inputImage);
            return tesseract.doOCR(preprocessedImage);
        } catch (TesseractException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private File preprocessForOCR(final File inputImage) throws IOException {
        try (Mat original = opencv_imgcodecs.imread(inputImage.getAbsolutePath())) {

            List<Function<Mat, Mat>> transformations = List.of(
                    this::toGrayscale,
                    this::toBinary,
                    this::applyMorphology,
                    this::addPadding
            );

            Mat preprocessed = applyTransformations(original, transformations);
            File temp = File.createTempFile("ocr-preprocessed", ".png");
            opencv_imgcodecs.imwrite(temp.getAbsolutePath(), preprocessed);
            preprocessed.close();
            return temp;
        }
    }

    private Mat toGrayscale(Mat input) {
        Mat gray = new Mat();
        opencv_imgproc.cvtColor(input, gray, opencv_imgproc.COLOR_BGR2GRAY);
        return gray;
    }

    private Mat toBinary(Mat gray) {
        Mat binary = new Mat();
        opencv_imgproc.threshold(gray, binary, 0, 255, opencv_imgproc.THRESH_BINARY + opencv_imgproc.THRESH_OTSU);
        opencv_core.bitwise_not(binary, binary);
        return binary;
    }

    private Mat applyMorphology(Mat binary) {
        Mat kernel = opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_RECT, new Size(SCALE_FACTOR, SCALE_FACTOR));
        Mat morphed = new Mat();
        opencv_imgproc.morphologyEx(binary, morphed, opencv_imgproc.MORPH_CLOSE, kernel);
        return morphed;
    }

    private Mat addPadding(Mat input) {
        Mat padded = new Mat();
        opencv_core.copyMakeBorder(input, padded, PADDING, PADDING, PADDING, PADDING, opencv_core.BORDER_CONSTANT, Scalar.BLACK);
        return padded;
    }

    private Mat upscale(Mat input) {
        Mat upscaled = new Mat();
        opencv_imgproc.resize(input, upscaled, new Size(input.cols() * SCALE_FACTOR, input.rows() * SCALE_FACTOR));
        return upscaled;
    }

    private Mat sharpen(Mat input) {
        Mat sharpened = new Mat();
        Mat kernel = new Mat(3, 3, opencv_core.CV_32F, new FloatPointer(
                0, -1, 0,
                -1, 5, -1,
                0, -1, 0
        ));
        opencv_imgproc.filter2D(input, sharpened, input.depth(), kernel);
        return sharpened;
    }

    private Mat applyTransformations(Mat input, List<Function<Mat, Mat>> pipeline) {
        Mat current = input;
        for (Function<Mat, Mat> step : pipeline) {
            Mat next = step.apply(current);
            if (current != input) {
                current.close();
            }
            current = next;
        }
        return current;
    }

}
