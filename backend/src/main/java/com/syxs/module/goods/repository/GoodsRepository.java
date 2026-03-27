package com.syxs.module.goods.repository;

import com.syxs.module.goods.entity.Goods;
import com.syxs.common.enums.GoodsStatus;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GoodsRepository extends JpaRepository<Goods, Long> {

    List<Goods> findAllByOrderByCreatedAtDesc();

    List<Goods> findAllBySellerPhoneOrderByCreatedAtDesc(String sellerPhone);

    List<Goods> findAllBySellerPhoneAndStatusOrderByCreatedAtDesc(String sellerPhone, GoodsStatus status);

    @Query("""
        select g
        from Goods g
        where (:keyword is null
            or lower(concat(
                coalesce(g.title, ''),
                ' ',
                coalesce(g.brand, ''),
                ' ',
                coalesce(g.category, ''),
                ' ',
                coalesce(g.story, ''),
                ' ',
                coalesce(g.description, '')
            )) like lower(concat('%', :keyword, '%')))
          and (:category is null or g.category = :category)
          and (:maxPrice is null or g.salePrice <= :maxPrice)
          and g.status = com.syxs.common.enums.GoodsStatus.ON_SALE
        """)
    List<Goods> searchCatalog(@Param("keyword") String keyword,
                              @Param("category") String category,
                              @Param("maxPrice") BigDecimal maxPrice);

    @Query("""
        select g.category as name, count(g) as count
        from Goods g
        where g.category is not null and trim(g.category) <> ''
        group by g.category
        order by count(g) desc, g.category asc
        """)
    List<GoodsCategoryStatProjection> listCategoryStats();

    interface GoodsCategoryStatProjection {
        String getName();

        Long getCount();
    }
}
