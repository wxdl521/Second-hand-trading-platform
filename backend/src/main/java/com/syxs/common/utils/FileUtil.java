package com.syxs.common.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public final class FileUtil {

    private FileUtil() {
    }

    public static String storeFile(Path uploadPath, MultipartFile file) throws IOException {
        Files.createDirectories(uploadPath);
        String filename = UUID.randomUUID() + "-" + file.getOriginalFilename();
        Path target = uploadPath.resolve(filename);
        file.transferTo(target);
        return filename;
    }
}
