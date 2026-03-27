package com.syxs.module.goods.controller;

import com.syxs.common.result.R;
import com.syxs.common.support.CurrentUserResolver;
import com.syxs.module.goods.dto.GoodsCreateDTO;
import com.syxs.module.goods.dto.GoodsCategoryVO;
import com.syxs.module.goods.dto.GoodsDetailVO;
import com.syxs.module.goods.dto.GoodsListVO;
import com.syxs.module.goods.service.GoodsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/goods")
public class GoodsController {

    private final GoodsService goodsService;
    private final CurrentUserResolver currentUserResolver;

    public GoodsController(GoodsService goodsService,
                           CurrentUserResolver currentUserResolver) {
        this.goodsService = goodsService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping
    public R<List<GoodsListVO>> list(@RequestParam(value = "q", required = false) String keyword,
                                     @RequestParam(value = "category", required = false) String category,
                                     @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
                                     @RequestParam(value = "sort", required = false) String sortMode) {
        return R.ok(goodsService.listGoods(keyword, category, maxPrice, sortMode));
    }

    @GetMapping("/categories")
    public R<List<GoodsCategoryVO>> categories() {
        return R.ok(goodsService.listCategories());
    }

    @GetMapping("/{id}")
    public R<GoodsDetailVO> detail(@PathVariable Long id) {
        return R.ok(goodsService.getDetail(id));
    }

    @PostMapping
    public R<GoodsDetailVO> create(@Valid @RequestBody GoodsCreateDTO request, HttpServletRequest httpRequest) {
        return R.ok(goodsService.createGoods(request, resolvePhone(httpRequest)));
    }

    @PutMapping("/{id}")
    public R<GoodsDetailVO> update(@PathVariable Long id,
                                   @Valid @RequestBody GoodsCreateDTO request,
                                   HttpServletRequest httpRequest) {
        return R.ok(goodsService.updateGoods(id, request, resolvePhone(httpRequest)));
    }

    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        goodsService.deleteGoods(id, resolvePhone(httpRequest));
        return R.ok(true);
    }

    @PostMapping("/{id}/publish")
    public R<GoodsDetailVO> publish(@PathVariable Long id, HttpServletRequest httpRequest) {
        return R.ok(goodsService.publishGoods(id, resolvePhone(httpRequest)));
    }

    @GetMapping("/my")
    public R<List<GoodsListVO>> myGoods(@RequestParam(value = "status", required = false) String status,
                                        HttpServletRequest httpRequest) {
        return R.ok(goodsService.listMyGoods(resolvePhone(httpRequest), status));
    }

    @PostMapping("/{id}/favorite")
    public R<Boolean> favorite(@PathVariable Long id, HttpServletRequest httpRequest) {
        goodsService.favoriteGoods(id, resolvePhone(httpRequest));
        return R.ok(true);
    }

    @DeleteMapping("/{id}/favorite")
    public R<Boolean> unfavorite(@PathVariable Long id, HttpServletRequest httpRequest) {
        goodsService.unfavoriteGoods(id, resolvePhone(httpRequest));
        return R.ok(true);
    }

    @GetMapping("/my/favorites")
    public R<List<GoodsListVO>> favorites(HttpServletRequest httpRequest) {
        return R.ok(goodsService.listFavoriteGoods(resolvePhone(httpRequest)));
    }

    private String resolvePhone(HttpServletRequest request) {
        return currentUserResolver.resolvePhone(request);
    }
}
