package dev.rubasace.linkedin.games_tracker.image;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;

import static org.bytedeco.opencv.global.opencv_core.BORDER_CONSTANT;
import static org.bytedeco.opencv.global.opencv_core.CV_32F;
import static org.bytedeco.opencv.global.opencv_core.bitwise_not;
import static org.bytedeco.opencv.global.opencv_core.copyMakeBorder;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.MORPH_CLOSE;
import static org.bytedeco.opencv.global.opencv_imgproc.MORPH_RECT;
import static org.bytedeco.opencv.global.opencv_imgproc.THRESH_BINARY;
import static org.bytedeco.opencv.global.opencv_imgproc.THRESH_OTSU;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.filter2D;
import static org.bytedeco.opencv.global.opencv_imgproc.getStructuringElement;
import static org.bytedeco.opencv.global.opencv_imgproc.morphologyEx;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;
import static org.bytedeco.opencv.global.opencv_imgproc.threshold;

@Component
public class ImageTextExtractor {

    public static final int PADDING = 40;
    private final Tesseract tesseract;

    public ImageTextExtractor() {
        //TODO fix when ready
        System.setProperty("jna.library.path", "/opt/homebrew/opt/tesseract/lib");
        this.tesseract = new Tesseract();

        URL resource = getClass().getClassLoader().getResource("tessdata");
        if (resource != null) {
            tesseract.setDatapath(new File(resource.getFile()).getAbsolutePath());
        } else {
            throw new RuntimeException("tessdata folder not found in resources!");
        }

        tesseract.setLanguage("eng+spa");
        tesseract.setPageSegMode(11);
        tesseract.setVariable("user_defined_dpi", "300");
    }

    public String extractText(final File inputImage) {
        try {
            File preprocessedImage = preprocessForOCR(inputImage);
            return tesseract.doOCR(preprocessedImage);
        } catch (TesseractException e) {
            throw new RuntimeException(e);
        }

    }

    @NotNull
    private static File preprocessForOCR(final File inputImage) {
        try (Mat original = imread(inputImage.getAbsolutePath())) {
            // Step 1: Grayscale
            Mat gray = new Mat();
            cvtColor(original, gray, COLOR_BGR2GRAY);

            // Step 2: Threshold
            Mat binary = new Mat();
            threshold(gray, binary, 0, 255, THRESH_BINARY + THRESH_OTSU);

            // Step 3: Invert if needed (optional, try both)
            bitwise_not(binary, binary);

            // Step 4: Morphology (optional)
            Mat kernel = getStructuringElement(MORPH_RECT, new Size(2, 2));
            morphologyEx(binary, binary, MORPH_CLOSE, kernel);

            // Step 5: Add padding
            copyMakeBorder(binary, binary, PADDING, PADDING, PADDING, PADDING, BORDER_CONSTANT, Scalar.BLACK);

            // Step 6: Upscale
            Mat upscaled = new Mat();
            resize(binary, upscaled, new Size(binary.cols() * 2, binary.rows() * 2));

            Mat sharpened = new Mat();
            Mat sharpKernel = new Mat(3, 3, CV_32F, new FloatPointer(
                    0, -1, 0,
                    -1, 5, -1,
                    0, -1, 0
            ));
            filter2D(upscaled, sharpened, upscaled.depth(), sharpKernel);

            Mat finalInput = upscaled;

            // Step 8: Write to temp file
            File temp = File.createTempFile("ocr-preprocessed", ".png");
            imwrite(temp.getAbsolutePath(), finalInput);
            return temp;
        } catch (Exception e) {
            throw new RuntimeException("OCR failed", e);
        }
    }

}
