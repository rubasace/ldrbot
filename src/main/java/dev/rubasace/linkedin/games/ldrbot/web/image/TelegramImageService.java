package dev.rubasace.linkedin.games.ldrbot.web.image;

import dev.rubasace.linkedin.games.ldrbot.assets.AssetsDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetUserProfilePhotos;
import org.telegram.telegrambots.meta.api.objects.UserProfilePhotos;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
public class TelegramImageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramImageService.class);

    private final TelegramClient telegramClient;
    private final AssetsDownloader assetsDownloader;

    public TelegramImageService(final TelegramClient telegramClient, final AssetsDownloader assetsDownloader) {
        this.telegramClient = telegramClient;
        //        this.cacheDir = Paths.get("cache/group-images");
        //        this.urlCache = Caffeine.newBuilder()
        //                                .expireAfterWrite(30, TimeUnit.MINUTES)
        //                                .maximumSize(1000)
        //                                .build();
        //
        //        try {
        //            Files.createDirectories(cacheDir);
        //        } catch (IOException e) {
        //            throw new RuntimeException("Unable to create cache directory", e);
        //        }
        this.assetsDownloader = assetsDownloader;
    }

    public Resource getUserImage(final Long userId) {
        try {
            GetUserProfilePhotos getPhotos = new GetUserProfilePhotos(userId);
            getPhotos.setUserId(userId);
            getPhotos.setLimit(5);

            UserProfilePhotos photos = telegramClient.execute(getPhotos);
            if (photos.getTotalCount() == 0) {
                return null;
            }

            java.io.File image = assetsDownloader.getImage(photos.getPhotos().getFirst());
            return new FileSystemResource(image);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

