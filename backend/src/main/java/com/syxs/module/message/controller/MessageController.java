package com.syxs.module.message.controller;

import com.syxs.common.result.R;
import com.syxs.common.support.CurrentUserResolver;
import com.syxs.module.message.entity.UserMessage;
import com.syxs.module.message.service.UserMessageService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/message")
public class MessageController {

    private final UserMessageService userMessageService;
    private final CurrentUserResolver currentUserResolver;

    public MessageController(UserMessageService userMessageService,
                             CurrentUserResolver currentUserResolver) {
        this.userMessageService = userMessageService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping("/list")
    public R<List<UserMessage>> list(HttpServletRequest request) {
        return R.ok(userMessageService.listMessages(resolvePhone(request)));
    }

    @PostMapping("/{id}/read")
    public R<UserMessage> markRead(@PathVariable Long id, HttpServletRequest request) {
        return R.ok(userMessageService.markRead(id, resolvePhone(request)));
    }

    private String resolvePhone(HttpServletRequest request) {
        return currentUserResolver.resolvePhone(request);
    }
}
