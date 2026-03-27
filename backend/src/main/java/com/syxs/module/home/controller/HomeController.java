package com.syxs.module.home.controller;

import com.syxs.common.result.R;
import com.syxs.module.home.dto.HomeLandingVO;
import com.syxs.module.home.service.HomeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping("/landing")
    public R<HomeLandingVO> landing() {
        return R.ok(homeService.getLanding());
    }
}
