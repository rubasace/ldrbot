package dev.rubasace.linkedin.games.ldrbot.image;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = ImageGameDurationExtractor.class)
@ImportAutoConfiguration(ConfigurationPropertiesAutoConfiguration.class)
@EnableConfigurationProperties(TesseractProperties.class)
class ImageTestConfiguration {

}