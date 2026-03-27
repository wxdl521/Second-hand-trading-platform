package com.syxs.module.goods.repository;

import com.syxs.module.goods.entity.GoodsImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoodsImageRepository extends JpaRepository<GoodsImage, Long> {

    List<GoodsImage> findByGoodsIdOrderBySortNoAsc(Long goodsId);

    void deleteByGoodsId(Long goodsId);
}
