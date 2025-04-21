package dev.rubasace.linkedin.games_tracker.image;

import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.converter.SimpleArgumentConverter;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.telegram.telegrambots.longpolling.starter.TelegramBotInitializer;

import java.io.File;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ImageDurationExtractorTest {

    @MockitoBean
    private TelegramBotInitializer telegramBotInitializer;

    @Autowired
    private ImageDurationExtractor imageDurationExtractor;

    @CsvSource({
            "1.jpeg,28s",
            "2.jpeg,47s",
            "3.jpeg,2m51s"
    })
    @ParameterizedTest
    void shouldExtractDuration(final String imageName, @ConvertWith(DurationConverter.class) final Duration expectedDuration) {

        File imageFile = new File("src/test/resources/images/" + imageName);
        try (Mat image = opencv_imgcodecs.imread(imageFile.getAbsolutePath())) {
            Optional<Duration> duration = imageDurationExtractor.extractDuration(image);
            assertEquals(Optional.of(expectedDuration), duration);
        }
    }

    private static class DurationConverter extends SimpleArgumentConverter {
        @Override
        protected Object convert(Object source, Class<?> targetType) throws ArgumentConversionException {
            if (targetType != Duration.class) {
                throw new ArgumentConversionException("Only Duration supported");
            }
            return Duration.parse("PT" + source.toString().toUpperCase()); // Converts "55s" -> PT55S, "2m51s" -> PT2M51S
        }
    }
}