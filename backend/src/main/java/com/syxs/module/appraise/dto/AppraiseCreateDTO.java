package com.syxs.module.appraise.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AppraiseCreateDTO {

    @NotBlank(message = "Goods title is required")
    private String goodsTitle;

    @NotBlank(message = "Mode is required")
    private String mode;

    @NotBlank(message = "Booking time is required")
    private String bookingTime;

    private String note;
}
