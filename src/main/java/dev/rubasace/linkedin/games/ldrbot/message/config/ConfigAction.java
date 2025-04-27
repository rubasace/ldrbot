package dev.rubasace.linkedin.games.ldrbot.message.config;

public interface ConfigAction {

    String getKey();

    String getTitle();

    static ConfigAction of(String key, String title) {
        return new ConfigAction() {
            @Override
            public String getKey() {
                return key;
            }

            @Override
            public String getTitle() {
                return title;
            }
        };
    }

}
