package com.syxs.module.ai.repository;

import com.syxs.module.ai.entity.AiEstimate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiEstimateRepository extends JpaRepository<AiEstimate, Long> {

    Optional<AiEstimate> findFirstByGoodsIdAndStatusOrderByUpdatedAtDescIdDesc(Long goodsId, String status);

    List<AiEstimate> findAllByGoodsIdOrderByUpdatedAtDescIdDesc(Long goodsId);
}
