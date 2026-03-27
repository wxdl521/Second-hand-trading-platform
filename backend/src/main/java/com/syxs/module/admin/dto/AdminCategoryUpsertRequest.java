package com.syxs.module.admin.dto;

import java.util.List;
import lombok.Data;

@Data
public class AdminCategoryUpsertRequest {

    private String name;
    private String description;
    private List<String> featuredBrands;
    private String coverImage;
}
