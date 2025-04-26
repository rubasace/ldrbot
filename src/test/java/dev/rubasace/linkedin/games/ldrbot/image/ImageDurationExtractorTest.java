package dev.rubasace.linkedin.games.ldrbot.image;

import dev.rubasace.linkedin.games.ldrbot.session.GameDuration;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import dev.rubasace.linkedin.games.ldrbot.util.BackpressureExecutors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.converter.SimpleArgumentConverter;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;

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
            "6.jpeg,ZIP,27s",
            "7.jpeg,ZIP,15s",
    })
    @ParameterizedTest
    void shouldExtractDuration(final String imageName, final GameType game, @ConvertWith(
            DurationConverter.class) final Duration expectedDuration) throws GameDurationExtractionException {

        File imageFile = new File("src/test/resources/images/" + imageName);
        Optional<GameDuration> gameDuration = imageGameDurationExtractor.extractGameDuration(imageFile, 1L, new UserInfo(-1L, "", "Test", ""));
        assertAll(
                () -> assertEquals(Optional.of(expectedDuration), gameDuration.map(GameDuration::duration)),
                () -> assertEquals(Optional.of(game), gameDuration.map(GameDuration::type))
        );


    }

    @Test
    void shouldAllowParallelization() {

        ExecutorService executorService = BackpressureExecutors.newBackPressureVirtualThreadPerTaskExecutor("test", 200);

        int taskCount = 25;

        List<CompletableFuture<Void>> futures = IntStream.range(0, taskCount)
                                                         .mapToObj(i -> CompletableFuture.runAsync(() -> {
                                                             try {
                                                                 File imageFile = new File("src/test/resources/images/1.jpeg");
                                                                 Optional<GameDuration> gameDuration = imageGameDurationExtractor.extractGameDuration(imageFile, 1L,
                                                                                                                                                      new UserInfo(-1L, "", "Test",
                                                                                                                                                                   ""));

                                                                 assertAll(
                                                                         () -> assertEquals(Optional.of(Duration.ofSeconds(28)), gameDuration.map(GameDuration::duration)),
                                                                         () -> assertEquals(Optional.of(GameType.QUEENS), gameDuration.map(GameDuration::type))
                                                                 );
                                                             } catch (Exception e) {
                                                                 throw new RuntimeException(e);
                                                             }
                                                         }, executorService))
                                                         .toList();

        futures.forEach(CompletableFuture::join);

        executorService.shutdown();
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