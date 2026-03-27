package com.syxs.module.order.repository;

import com.syxs.module.order.entity.Order;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findAllByOrderByCreatedAtDesc();

    List<Order> findAllByBuyerPhoneOrderByCreatedAtDesc(String buyerPhone);

    List<Order> findAllBySellerPhoneOrderByCreatedAtDesc(String sellerPhone);

    void deleteByGoodsId(Long goodsId);
}
