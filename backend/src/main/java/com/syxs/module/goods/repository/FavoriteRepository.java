package com.syxs.module.goods.repository;

import com.syxs.module.goods.entity.Favorite;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserIdAndGoodsId(Long userId, Long goodsId);

    List<Favorite> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    long countByGoodsId(Long goodsId);

    void deleteByUserIdAndGoodsId(Long userId, Long goodsId);
}
