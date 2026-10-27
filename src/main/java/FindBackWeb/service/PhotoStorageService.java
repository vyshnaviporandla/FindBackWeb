package FindBackWeb.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.UUID;

@Service
public class PhotoStorageService {
    public static final long MAX_PHOTO_BYTES = 5L * 1024L * 1024L;

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final Path uploadDirectory = Path.of(
            System.getProperty("user.home"), "FindBackWeb-uploads"
    ).toAbsolutePath().normalize();

    public Path getUploadDirectory() {
        return uploadDirectory;
    }

    /** Deletes only a generated image inside this application's upload directory. */
    public void delete(String publicPath) {
        if (publicPath == null || !publicPath.startsWith("/uploads/")) return;
        String fileName = publicPath.substring("/uploads/".length());
        if (fileName.isBlank() || fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) return;
        Path target = uploadDirectory.resolve(fileName).normalize();
        if (!target.startsWith(uploadDirectory)) return;
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // A stale file is safer than failing the user's report update.
        }
    }

    /** Stores an allow-listed image under a generated name, never the user's filename. */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (file.getSize() > MAX_PHOTO_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE, "The image must be 5 MB or smaller"
            );
        }

        String contentType = file.getContentType();
        String extension = contentType == null ? null : EXTENSIONS.get(contentType.toLowerCase());
        if (extension == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Only JPG, PNG, and WebP images are allowed"
            );
        }

        String generatedName = UUID.randomUUID() + extension;
        Path destination = uploadDirectory.resolve(generatedName).normalize();
        if (!destination.startsWith(uploadDirectory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image path");
        }

        try {
            Files.createDirectories(uploadDirectory);
            Files.write(destination, file.getBytes(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return "/uploads/" + generatedName;
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "The photo could not be saved. Please try again.", exception
            );
        }
    }
}
