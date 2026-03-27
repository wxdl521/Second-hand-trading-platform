package com.syxs.module.order.controller;

import com.syxs.common.support.CurrentUserResolver;
import com.syxs.common.result.R;
import com.syxs.module.order.entity.Order;
import com.syxs.module.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
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
    public R<List<Order>> list(HttpServletRequest request) {
        return R.ok(orderService.listOrders(resolvePhone(request)));
    }

    @GetMapping("/my/buy")
    public R<List<Order>> myBuyOrders(HttpServletRequest request) {
        return R.ok(orderService.listBuyOrders(resolvePhone(request)));
    }

    @GetMapping("/my/sell")
    public R<List<Order>> mySellOrders(HttpServletRequest request) {
        return R.ok(orderService.listSellOrders(resolvePhone(request)));
    }

    @GetMapping("/{id}")
    public R<Order> detail(@PathVariable Long id, HttpServletRequest request) {
        return R.ok(orderService.getOrder(id, resolvePhone(request)));
    }

    @PostMapping("/pay/{id}")
    public R<Order> pay(@PathVariable Long id, HttpServletRequest request) {
        return R.ok(orderService.payOrder(id, resolvePhone(request)));
    }

    @PostMapping("/ship/{id}")
    public R<Order> ship(@PathVariable Long id, HttpServletRequest request) {
        return R.ok(orderService.shipOrder(id, resolvePhone(request)));
    }

    @PostMapping("/confirm/{id}")
    public R<Order> confirm(@PathVariable Long id, HttpServletRequest request) {
        return R.ok(orderService.confirmOrder(id, resolvePhone(request)));
    }

    @PostMapping("/{id}/refund")
    public R<Order> refund(@PathVariable Long id, HttpServletRequest request) {
        return R.ok(orderService.refundOrder(id, resolvePhone(request)));
    }

    @PostMapping("/create")
    public R<Order> create(@RequestBody Map<String, Long> request, HttpServletRequest httpRequest) {
        return R.ok(orderService.createOrder(request.getOrDefault("goodsId", 1L), resolvePhone(httpRequest)));
    }

    private String resolvePhone(HttpServletRequest request) {
        return currentUserResolver.resolvePhone(request);
    }
}
