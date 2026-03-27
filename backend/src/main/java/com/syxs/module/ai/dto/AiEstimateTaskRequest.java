package com.syxs.module.ai.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Data;

@Data
public class AiEstimateTaskRequest {

    private Long goodsId;

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
