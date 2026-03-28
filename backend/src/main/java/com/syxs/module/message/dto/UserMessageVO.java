package com.syxs.module.message.dto;

import com.syxs.module.message.entity.UserMessage;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserMessageVO {

    private Long id;
    private String type;
    private String title;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public static UserMessageVO from(UserMessage message) {
        return UserMessageVO.builder()
            .id(message.getId())
            .type(message.getType())
            .title(message.getTitle())
            .content(message.getContent())
            .isRead(message.getIsRead())
            .createdAt(message.getCreatedAt())
            .readAt(message.getReadAt())
            .build();
    }
}
