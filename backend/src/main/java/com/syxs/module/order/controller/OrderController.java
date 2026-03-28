package com.syxs.module.order.controller;

import com.syxs.common.support.CurrentUserResolver;
import com.syxs.common.result.R;
import com.syxs.module.order.dto.OrderVO;
import com.syxs.module.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;
    private final CurrentUserResolver currentUserResolver;

    public OrderController(OrderService orderService, CurrentUserResolver currentUserResolver) {
        this.orderService = orderService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping("/list")
    public R<List<OrderVO>> list(HttpServletRequest request) {
        return R.ok(orderService.listOrders(resolvePhone(request)).stream()
            .map(OrderVO::from)
            .toList());
    }

    @GetMapping("/my/buy")
    public R<List<OrderVO>> myBuyOrders(HttpServletRequest request) {
        return R.ok(orderService.listBuyOrders(resolvePhone(request)).stream()
            .map(OrderVO::from)
            .toList());
    }

    @GetMapping("/my/sell")
    public R<List<OrderVO>> mySellOrders(HttpServletRequest request) {
        return R.ok(orderService.listSellOrders(resolvePhone(request)).stream()
            .map(OrderVO::from)
            .toList());
    }

    @GetMapping("/{id}")
    public R<OrderVO> detail(@PathVariable Long id, HttpServletRequest request) {
        return R.ok(OrderVO.from(orderService.getOrder(id, resolvePhone(request))));
    }

    @PostMapping({"/pay/{id}", "/{id}/pay"})
    public R<OrderVO> pay(@PathVariable Long id, HttpServletRequest request) {
        return R.ok(OrderVO.from(orderService.payOrder(id, resolvePhone(request))));
    }

    @PostMapping({"/ship/{id}", "/{id}/ship"})
    public R<OrderVO> ship(@PathVariable Long id, HttpServletRequest request) {
        return R.ok(OrderVO.from(orderService.shipOrder(id, resolvePhone(request))));
    }

    @PostMapping({"/confirm/{id}", "/{id}/confirm"})
    public R<OrderVO> confirm(@PathVariable Long id, HttpServletRequest request) {
        return R.ok(OrderVO.from(orderService.confirmOrder(id, resolvePhone(request))));
    }

    @PostMapping("/{id}/refund")
    public R<OrderVO> refund(@PathVariable Long id, HttpServletRequest request) {
        return R.ok(OrderVO.from(orderService.refundOrder(id, resolvePhone(request))));
    }

    @PostMapping({"", "/create"})
    public R<OrderVO> create(@RequestBody Map<String, Long> request, HttpServletRequest httpRequest) {
        return R.ok(OrderVO.from(orderService.createOrder(request.getOrDefault("goodsId", 1L), resolvePhone(httpRequest))));
    }

    private String resolvePhone(HttpServletRequest request) {
        return currentUserResolver.resolvePhone(request);
    }
}
