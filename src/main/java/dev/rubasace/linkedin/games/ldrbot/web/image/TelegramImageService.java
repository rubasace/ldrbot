package dev.rubasace.linkedin.games.ldrbot.web.image;

import dev.rubasace.linkedin.games.ldrbot.assets.AssetsDownloader;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUser;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.telegram.telegrambots.meta.api.methods.GetUserProfilePhotos;
import org.telegram.telegrambots.meta.api.objects.UserProfilePhotos;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.File;

@Component
public class TelegramImageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramImageService.class);

    private final TelegramClient telegramClient;
    private final AssetsDownloader assetsDownloader;
    private final TelegramUserService telegramUserService;
    private final RestClient restClient;

    public TelegramImageService(final TelegramClient telegramClient, final AssetsDownloader assetsDownloader, final TelegramUserService telegramUserService, final RestClient.Builder builder) {
        this.telegramClient = telegramClient;
        this.assetsDownloader = assetsDownloader;
        this.telegramUserService = telegramUserService;
        this.restClient = builder.clone()
                                 .baseUrl("https://api.dicebear.com/7.x/initials/")
                                 .build();
    }

    public ImageData getUserImage(final Long userId) {
        try {
            GetUserProfilePhotos getPhotos = new GetUserProfilePhotos(userId);
            getPhotos.setUserId(userId);
            getPhotos.setLimit(5);

            UserProfilePhotos photos = telegramClient.execute(getPhotos);
            if (photos.getTotalCount() == 0) {
                return getFallBackAvatar(userId);
            }

            File image = assetsDownloader.getImage(photos.getPhotos().getFirst());
            return new ImageData(new FileSystemResource(image), MediaType.parseMediaType("image/avif"));

        } catch (Exception e) {
            LOGGER.error("Error getting user image", e);
            return getFallBackAvatar(userId);
        }
    }

    private ImageData getFallBackAvatar(final Long userId) {
        TelegramUser telegramUser = telegramUserService.find(userId).orElseThrow();
        String name = StringUtils.hasText(telegramUser.getUserName()) ? telegramUser.getUserName() + telegramUser.getFirstName() : telegramUser.getFirstName();

        byte[] bytes = restClient.get()
                                 .uri(uriBuilder -> uriBuilder
                                         .path("svg")
                                         .queryParam("seed", name)
                                         .queryParam("chars", "1")
                                         .build())
                                 .retrieve()
                                 .body(byte[].class);

        return new ImageData(new ByteArrayResource(bytes), MediaType.parseMediaType("image/svg+xml"));
    }

    record ImageData(Resource resource, MediaType mediaType) {
    }
}

