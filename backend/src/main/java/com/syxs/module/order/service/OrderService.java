package com.syxs.module.order.service;

import com.syxs.module.order.entity.Order;
import java.util.List;

public interface OrderService {

    List<Order> listOrders(String operatorPhone);

    List<Order> listBuyOrders(String operatorPhone);

    List<Order> listSellOrders(String operatorPhone);

    Order getOrder(Long id, String operatorPhone);

    Order createOrder(Long goodsId, String operatorPhone);

    Order payOrder(Long id, String operatorPhone);

    Order shipOrder(Long id, String operatorPhone);

    Order confirmOrder(Long id, String operatorPhone);

    Order refundOrder(Long id, String operatorPhone);
}
