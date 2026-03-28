package com.syxs.module.ai.controller;

import com.syxs.common.result.R;
import com.syxs.common.support.CurrentUserResolver;
import com.syxs.module.ai.dto.AiEstimateTaskRequest;
import com.syxs.module.ai.dto.AiEstimateTaskVO;
import com.syxs.module.ai.service.AiEstimateService;
import com.syxs.module.file.service.LocalFileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/ai")
public class AiEstimateController {

    private final AiEstimateService aiEstimateService;
    private final LocalFileStorageService localFileStorageService;
    private final CurrentUserResolver currentUserResolver;

    public AiEstimateController(AiEstimateService aiEstimateService,
                                LocalFileStorageService localFileStorageService,
                                CurrentUserResolver currentUserResolver) {
        this.aiEstimateService = aiEstimateService;
        this.localFileStorageService = localFileStorageService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/estimate")
    public R<Map<String, Object>> estimate(@RequestBody EstimateRequest request) {
        return R.ok(
            aiEstimateService.estimate(
                request.getTitle(),
                request.getCategory(),
                request.getBrand(),
                request.getCondition(),
                request.getDescription(),
                request.getImageUrls(),
                request.getYearsUsed(),
                request.getRarity()
            )
        );
    }

    @PostMapping(value = "/estimate/upload", consumes = MediaType.APPLICATION_JSON_VALUE)
    public R<AiEstimateTaskVO> createTask(@Validated @RequestBody AiEstimateTaskRequest request,
                                          HttpServletRequest httpRequest) {
        return R.ok(aiEstimateService.createTask(request, resolvePhone(httpRequest)));
    }

    @PostMapping(value = "/estimate/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String filename = localFileStorageService.store(file);
        return R.ok(Map.of("filename", filename, "url", "/files/" + filename));
    }

    @GetMapping("/estimate/{estimateId}")
    public R<AiEstimateTaskVO> task(@PathVariable Long estimateId, HttpServletRequest request) {
        return R.ok(aiEstimateService.getTask(estimateId, resolvePhone(request)));
    }

    @GetMapping("/estimate/goods/{goodsId}")
    public R<AiEstimateTaskVO> latestGoodsTask(@PathVariable Long goodsId, HttpServletRequest request) {
        return R.ok(aiEstimateService.getLatestTaskForGoods(goodsId, resolvePhone(request)));
    }

    private String resolvePhone(HttpServletRequest request) {
        return currentUserResolver.resolvePhone(request);
    }

    @Data
    public static class EstimateRequest {
        @NotBlank
        private String title;
        @NotBlank
        private String category;
        private String brand;
        private String condition;
        private String description;
        private List<String> imageUrls = List.of();
        @Min(0)
        private int yearsUsed = 1;
        @Min(1)
        private int rarity = 3;
    }
}
