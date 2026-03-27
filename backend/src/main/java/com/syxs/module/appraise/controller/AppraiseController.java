package com.syxs.module.appraise.controller;

import com.syxs.common.support.CurrentUserResolver;
import com.syxs.common.result.R;
import com.syxs.module.appraise.entity.AppraiseOrder;
import com.syxs.module.appraise.service.AppraiseService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appraise")
public class AppraiseController {

    private final AppraiseService appraiseService;
    private final CurrentUserResolver currentUserResolver;

    public AppraiseController(AppraiseService appraiseService,
                              CurrentUserResolver currentUserResolver) {
        this.appraiseService = appraiseService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping("/list")
    public R<List<AppraiseOrder>> list(HttpServletRequest request) {
        return R.ok(appraiseService.list(resolvePhone(request)));
    }

    @PostMapping("/create")
    public R<AppraiseOrder> create(@RequestBody AppraiseOrder request, HttpServletRequest httpRequest) {
        return R.ok(appraiseService.create(request, resolvePhone(httpRequest)));
    }

    private String resolvePhone(HttpServletRequest request) {
        return currentUserResolver.resolvePhone(request);
    }
}
