package com.syxs.module.appraise.service.impl;

import com.syxs.module.appraise.entity.AppraiseOrder;
import com.syxs.module.appraise.repository.AppraiseOrderRepository;
import com.syxs.module.appraise.service.AppraiseService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AppraiseServiceImpl implements AppraiseService {

    private final AppraiseOrderRepository appraiseOrderRepository;

    public AppraiseServiceImpl(AppraiseOrderRepository appraiseOrderRepository) {
        this.appraiseOrderRepository = appraiseOrderRepository;
    }

    @Override
    public List<AppraiseOrder> list(String userPhone) {
        return appraiseOrderRepository.findByUserPhoneOrderByCreatedAtDesc(userPhone);
    }

    @Override
    public AppraiseOrder create(AppraiseOrder request, String userPhone) {
        AppraiseOrder order = new AppraiseOrder();
        order.setGoodsTitle(request.getGoodsTitle());
        order.setMode(request.getMode());
        order.setBookingTime(request.getBookingTime());
        order.setNote(request.getNote());
        order.setUserPhone(userPhone);
        order.setStatus(resolveInitialStatus(request.getMode(), request.getBookingTime()));
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        return appraiseOrderRepository.save(order);
    }

    private String resolveInitialStatus(String mode, String bookingTime) {
        if (mode == null || mode.isBlank()) {
            return "PENDING_CONFIRMATION";
        }
        if (isAiMode(mode)) {
            return "CONFIRMED";
        }

        int slotCapacity = isVideoMode(mode) ? 6 : 4;
        long queuedCount = appraiseOrderRepository.countByModeAndBookingTimeAndStatusIn(
            mode,
            bookingTime,
            List.of("PENDING_CONFIRMATION", "CONFIRMED")
        );
        return queuedCount >= slotCapacity ? "PENDING_CONFIRMATION" : "CONFIRMED";
    }

    private boolean isAiMode(String mode) {
        String normalized = mode.toLowerCase();
        return normalized.contains("ai") || normalized.contains("quick");
    }

    private boolean isVideoMode(String mode) {
        String normalized = mode.toLowerCase();
        return normalized.contains("video") || mode.contains("视频") || mode.contains("瑙嗛");
    }
}
