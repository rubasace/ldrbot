package dev.rubasace.linkedin.games_tracker.image;

import dev.rubasace.linkedin.games_tracker.session.GameDuration;
import dev.rubasace.linkedin.games_tracker.session.GameType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.converter.SimpleArgumentConverter;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = ImageTestConfiguration.class)
class ImageDurationExtractorTest {

    @Autowired
    private ImageGameDurationExtractor imageGameDurationExtractor;

    @CsvSource({
            "1.jpeg,QUEENS,28s",
            "2.jpeg,TANGO,47s",
            "3.jpeg,ZIP,2m51s",
            "4.jpeg,QUEENS,23s",
            "5.jpeg,TANGO,1m20s",
            "6.jpeg,ZIP,27s"
    })
    @ParameterizedTest
    void shouldExtractDuration(final String imageName, final GameType game, @ConvertWith(
            DurationConverter.class) final Duration expectedDuration) throws GameDurationExtractionException {

        File imageFile = new File("src/test/resources/images/" + imageName);
        Optional<GameDuration> gameDuration = imageGameDurationExtractor.extractGameDuration(imageFile, 1L, "test");
        assertAll(
                () -> assertEquals(Optional.of(expectedDuration), gameDuration.map(GameDuration::duration)),
                () -> assertEquals(Optional.of(game), gameDuration.map(GameDuration::type))
        );


    }

    private static class DurationConverter extends SimpleArgumentConverter {
        @Override
        protected Object convert(Object source, Class<?> targetType) throws ArgumentConversionException {
            if (targetType != Duration.class) {
                throw new ArgumentConversionException("Only Duration supported");
            }
            return Duration.parse("PT" + source.toString().toUpperCase());
        }
    }
}