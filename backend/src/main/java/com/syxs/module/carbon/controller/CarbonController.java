package com.syxs.module.carbon.controller;

import com.syxs.common.support.CurrentUserResolver;
import com.syxs.common.result.R;
import com.syxs.module.carbon.entity.CarbonRecord;
import com.syxs.module.carbon.service.CarbonService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carbon")
public class CarbonController {

    private final CarbonService carbonService;
    private final CurrentUserResolver currentUserResolver;

    public CarbonController(CarbonService carbonService, CurrentUserResolver currentUserResolver) {
        this.carbonService = carbonService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping("/summary")
    public R<Map<String, Object>> summary(HttpServletRequest request) {
        return R.ok(carbonService.summary(resolvePhone(request)));
    }

    @GetMapping("/account")
    public R<Map<String, Object>> account(HttpServletRequest request) {
        return R.ok(carbonService.account(resolvePhone(request)));
    }

    @GetMapping("/records")
    public R<List<CarbonRecord>> records(HttpServletRequest request) {
        return R.ok(carbonService.records(resolvePhone(request)));
    }

    @PostMapping(value = "/certificate", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> certificate(HttpServletRequest request) {
        return ResponseEntity.ok(carbonService.certificate(resolvePhone(request)));
    }

    private String resolvePhone(HttpServletRequest request) {
        return currentUserResolver.resolvePhone(request);
    }
}
