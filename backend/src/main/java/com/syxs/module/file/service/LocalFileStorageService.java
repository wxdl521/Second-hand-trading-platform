package com.syxs.module.file.service;

import com.syxs.common.utils.FileUtil;
import com.syxs.config.FileStorageConfig;
import java.io.IOException;
import java.nio.file.Path;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalFileStorageService {

    private final FileStorageConfig fileStorageConfig;

    public LocalFileStorageService(FileStorageConfig fileStorageConfig) {
        this.fileStorageConfig = fileStorageConfig;
    }

    public String store(MultipartFile file) throws IOException {
        return FileUtil.storeFile(Path.of(fileStorageConfig.getUploadDir()), file);
    }
}
