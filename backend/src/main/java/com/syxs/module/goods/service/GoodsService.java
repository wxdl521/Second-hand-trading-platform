package com.syxs.module.goods.service;

import com.syxs.module.goods.dto.GoodsCreateDTO;
import com.syxs.module.goods.dto.GoodsCategoryVO;
import com.syxs.module.goods.dto.GoodsDetailVO;
import com.syxs.module.goods.dto.GoodsListVO;
import java.math.BigDecimal;
import java.util.List;

public interface GoodsService {

    List<GoodsListVO> listGoods(String keyword, String category, BigDecimal maxPrice, String sortMode);

    List<GoodsCategoryVO> listCategories();

    GoodsDetailVO getDetail(Long id);

    GoodsDetailVO createGoods(GoodsCreateDTO request, String operatorPhone);

    GoodsDetailVO updateGoods(Long id, GoodsCreateDTO request, String operatorPhone);

    GoodsDetailVO publishGoods(Long id, String operatorPhone);

    void deleteGoods(Long id, String operatorPhone);

    List<GoodsListVO> listMyGoods(String operatorPhone, String status);

    void favoriteGoods(Long id, String operatorPhone);

    void unfavoriteGoods(Long id, String operatorPhone);

    List<GoodsListVO> listFavoriteGoods(String operatorPhone);
}
