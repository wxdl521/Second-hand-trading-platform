package com.syxs.module.goods.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class GoodsCreateDTO {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Brand is required")
    private String brand;

    @NotBlank(message = "Condition level is required")
    private String conditionLevel;

    @NotNull(message = "Sale price is required")
    private BigDecimal salePrice;

    private String story;

    private String tags;

    private String description;

    private List<String> imageUrls;
}
