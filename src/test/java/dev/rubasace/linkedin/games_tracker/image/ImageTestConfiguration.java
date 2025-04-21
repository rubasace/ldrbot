package dev.rubasace.linkedin.games_tracker.image;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;

@EnableConfigurationProperties(TesseractProperties.class)
@TestConfiguration
@ComponentScan
class ImageTestConfiguration {

}