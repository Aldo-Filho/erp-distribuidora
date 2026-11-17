package com.pi.erp.company;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LogoStorage {

    private static final Logger logger = LoggerFactory.getLogger(LogoStorage.class);
    public static final int MAX_BYTES = 2 * 1024 * 1024;
    private final Path directory;

    public LogoStorage(
        @Value("${erp.appearance.logo-directory:${user.home}/.jamshub/appearance}") String directory
    ) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    public String save(MultipartFile file) {
        if (
            file == null || file.isEmpty() || file.getSize() > MAX_BYTES
        ) throw new IllegalArgumentException("Selecione um logo PNG ou JPEG de até 2 MB.");
        String format = switch (
            file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT)
        ) {
            case "image/png" -> "png";
            case "image/jpeg" -> "jpeg";
            default -> throw new IllegalArgumentException(
                "O logo deve ser uma imagem PNG ou JPEG."
            );
        };
        try {
            byte[] original;
            try (var stream = file.getInputStream()) {
                original = stream.readNBytes(MAX_BYTES + 1);
            }
            if (original.length > MAX_BYTES) throw new IllegalArgumentException(
                "O logo deve ter até 2 MB."
            );
            byte[] normalized;
            // Decode and re-encode instead of trusting the filename, MIME header or metadata.
            try (
                ImageInputStream input = ImageIO.createImageInputStream(
                    new java.io.ByteArrayInputStream(original)
                )
            ) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new IllegalArgumentException(
                    "Não foi possível ler a imagem do logo."
                );
                ImageReader reader = readers.next();
                try {
                    if (
                        !reader.getFormatName().equalsIgnoreCase(format)
                    ) throw new IllegalArgumentException(
                        "O conteúdo do logo não corresponde ao tipo de imagem informado."
                    );
                    reader.setInput(input, true, true);
                    int width = reader.getWidth(0),
                        height = reader.getHeight(0);
                    if (
                        width < 1 ||
                        height < 1 ||
                        width > 4096 ||
                        height > 4096 ||
                        (long) width * height > 16_000_000
                    ) throw new IllegalArgumentException(
                        "O logo deve ter até 4096 pixels por lado e 16 milhões de pixels."
                    );
                    BufferedImage image = reader.read(0);
                    var output = new ByteArrayOutputStream();
                    if (!ImageIO.write(image, format, output)) throw new IllegalArgumentException(
                        "Imagem de logo inválida."
                    );
                    normalized = output.toByteArray();
                } finally {
                    reader.dispose();
                }
            }
            if (normalized.length > MAX_BYTES) throw new IllegalArgumentException(
                "A imagem processada supera 2 MB. Reduza o tamanho do logo."
            );
            return write(normalized, format);
        } catch (IOException issue) {
            throw new IllegalArgumentException(
                "Não foi possível ler a imagem. Selecione um PNG ou JPEG válido."
            );
        }
    }

    private String write(byte[] bytes, String format) {
        String filename = UUID.randomUUID() + (format.equals("png") ? ".png" : ".jpg");
        try {
            Files.createDirectories(directory);
            if (Files.isSymbolicLink(directory)) throw new IOException(
                "Logo directory cannot be a symbolic link."
            );
            Files.write(
                resolve(filename),
                bytes,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
            );
            return filename;
        } catch (java.nio.file.FileAlreadyExistsException issue) {
            throw new IllegalStateException("Não foi possível gerar a referência do logo.", issue);
        } catch (IOException issue) {
            discard(filename);
            throw new IllegalStateException("Não foi possível armazenar o logo.", issue);
        }
    }

    public byte[] read(String filename) {
        try (
            var stream = Files.newInputStream(
                resolve(filename),
                StandardOpenOption.READ,
                LinkOption.NOFOLLOW_LINKS
            )
        ) {
            byte[] bytes = stream.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) throw new IOException("Logo exceeds storage limit.");
            return bytes;
        } catch (IOException issue) {
            throw new IllegalStateException("Não foi possível ler o logo armazenado.", issue);
        }
    }

    public void discard(String filename) {
        if (filename == null) return;
        try {
            Files.deleteIfExists(resolve(filename));
        } catch (IOException issue) {
            logger.warn("Could not remove a replaced logo file.");
        }
    }

    private Path resolve(String filename) {
        if (
            filename == null ||
            !filename.matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(png|jpg)"
            )
        ) throw new IllegalStateException("Invalid stored logo reference.");
        Path result = directory.resolve(filename).normalize();
        if (!result.startsWith(directory)) throw new IllegalStateException(
            "Invalid logo location."
        );
        return result;
    }
}
