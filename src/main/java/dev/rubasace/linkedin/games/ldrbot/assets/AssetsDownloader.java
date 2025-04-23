package dev.rubasace.linkedin.games.ldrbot.assets;

import dev.rubasace.linkedin.games.ldrbot.configuration.TelegramBotProperties;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Comparator;
import java.util.List;

@Component
public class AssetsDownloader {

    private final TelegramClient telegramClient;

    AssetsDownloader(final TelegramClient telegramClient, final TelegramBotProperties telegramBotProperties) {
        this.telegramClient = telegramClient;

    }

    public java.io.File getImage(final List<PhotoSize> photoSizeList) {
        PhotoSize largestPhoto = photoSizeList.stream()
                                              .max(Comparator.comparing(PhotoSize::getFileSize))
                                              .orElseThrow();
        String fileId = largestPhoto.getFileId();
        try {
            File file = telegramClient.execute(new GetFile(fileId));
            return telegramClient.downloadFile(file);

        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private List<PhotoSize> getPhotos(final Message message) {
        if (message.hasPhoto()) {
            return message.getPhoto();
        } else if (message.hasDocument() && message.getDocument().getThumbnail() != null) {
            return List.of(message.getDocument().getThumbnail());
        }
        return List.of();
    }
}
