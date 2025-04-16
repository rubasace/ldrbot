package dev.rubasace.linkedin.games_tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class GamesTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GamesTrackerApplication.class, args);
    }

}
