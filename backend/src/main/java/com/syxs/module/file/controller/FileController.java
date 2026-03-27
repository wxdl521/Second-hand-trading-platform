package com.syxs.module.file.controller;

import com.syxs.common.result.R;
import com.syxs.module.file.service.LocalFileStorageService;
import java.io.IOException;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/file")
public class FileController {

    private final LocalFileStorageService localFileStorageService;

    public FileController(LocalFileStorageService localFileStorageService) {
        this.localFileStorageService = localFileStorageService;
    }

    @PostMapping("/upload")
    public R<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String filename = localFileStorageService.store(file);
        return R.ok(Map.of("filename", filename, "url", "/files/" + filename));
    }
}
