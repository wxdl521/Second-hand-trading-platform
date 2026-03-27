package com.syxs.module.appraise.repository;

import com.syxs.module.appraise.entity.AppraiseOrder;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppraiseOrderRepository extends JpaRepository<AppraiseOrder, Long> {

    List<AppraiseOrder> findByUserPhoneOrderByCreatedAtDesc(String userPhone);

    long countByModeAndBookingTimeAndStatusIn(String mode, String bookingTime, Collection<String> statuses);
}
