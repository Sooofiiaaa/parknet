package com.parknet.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ListingImageStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L;

    private final Path listingUploadDirectory;

    public ListingImageStorageService(@Value("${parknet.upload.listings-dir:uploads/listings}") String uploadDirectory) {
        this.listingUploadDirectory = Paths.get(uploadDirectory).toAbsolutePath().normalize();
    }

    public String storeListingImage(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return null;
        }
        if (imageFile.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new InvalidImageException("Снимката трябва да е до 5MB.");
        }

        String extension = extensionFrom(imageFile.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidImageException("Поддържат се само JPG, JPEG, PNG и WEBP снимки.");
        }

        try {
            Files.createDirectories(listingUploadDirectory);
            String storedFilename = UUID.randomUUID() + "." + extension;
            Path destination = listingUploadDirectory.resolve(storedFilename).normalize();
            if (!destination.startsWith(listingUploadDirectory)) {
                throw new InvalidImageException("Невалиден файл за качване.");
            }
            try (InputStream inputStream = imageFile.getInputStream()) {
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/listings/" + storedFilename;
        } catch (IOException ex) {
            throw new InvalidImageException("Снимката не може да бъде качена.", ex);
        }
    }

    private String extensionFrom(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new InvalidImageException("Файлът трябва да има разширение.");
        }
        String normalizedName = originalFilename.trim();
        int dotIndex = normalizedName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == normalizedName.length() - 1) {
            throw new InvalidImageException("Файлът трябва да има валидно разширение.");
        }
        return normalizedName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
