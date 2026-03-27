package com.syxs.module.ai.service;

import com.syxs.common.exception.BusinessException;
import com.syxs.common.utils.CarbonCalcUtil;
import com.syxs.module.ai.dto.AiEstimateTaskRequest;
import com.syxs.module.ai.dto.AiEstimateTaskVO;
import com.syxs.module.ai.entity.AiEstimate;
import com.syxs.module.ai.repository.AiEstimateRepository;
import com.syxs.module.goods.repository.GoodsRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiEstimateService {

    private final RuleBasedPricingEngine pricingEngine;
    private final OllamaClient ollamaClient;
    private final AiEstimateRepository aiEstimateRepository;
    private final GoodsRepository goodsRepository;

    public AiEstimateService(RuleBasedPricingEngine pricingEngine,
                             OllamaClient ollamaClient,
                             AiEstimateRepository aiEstimateRepository,
                             GoodsRepository goodsRepository) {
        this.pricingEngine = pricingEngine;
        this.ollamaClient = ollamaClient;
        this.aiEstimateRepository = aiEstimateRepository;
        this.goodsRepository = goodsRepository;
    }

    @Cacheable(
        value = "estimate",
        key = "#title + ':' + #category + ':' + #brand + ':' + #condition + ':' + #yearsUsed + ':' + #rarity + ':' + (#imageUrls == null ? 0 : #imageUrls.size())"
    )
    public Map<String, Object> estimate(String title,
                                        String category,
                                        String brand,
                                        String condition,
                                        String description,
                                        List<String> imageUrls,
                                        int yearsUsed,
                                        int rarity) {
        List<String> safeImageUrls = imageUrls == null ? List.of() : imageUrls.stream()
            .filter(url -> url != null && !url.isBlank())
            .limit(4)
            .toList();
        int imageCount = safeImageUrls.size();

        BigDecimal basePrice = pricingEngine.estimate(category, yearsUsed, rarity);
        BigDecimal price = basePrice
            .multiply(resolveBrandFactor(brand))
            .multiply(resolveConditionFactor(condition))
            .multiply(resolveImageFactor(imageCount))
            .multiply(resolveDescriptionFactor(description))
            .setScale(0, RoundingMode.HALF_UP);
        int confidence = resolveConfidence(yearsUsed, rarity, condition, description, imageCount);
        BigDecimal spread = BigDecimal.valueOf(Math.max(0.06d, 0.16d - confidence / 1000d));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", title);
        result.put("category", category);
        result.put("brand", brand);
        result.put("estimatePrice", price);
        result.put("lowPrice", price.multiply(BigDecimal.ONE.subtract(spread)).setScale(0, RoundingMode.HALF_UP));
        result.put("highPrice", price.multiply(BigDecimal.ONE.add(spread)).setScale(0, RoundingMode.HALF_UP));
        result.put("confidence", confidence);
        result.put("carbonSavedKg", CarbonCalcUtil.estimateCarbonSaved(price.doubleValue()));
        result.put(
            "summary",
            ollamaClient.estimateSummary(
                title,
                category,
                brand,
                condition,
                yearsUsed,
                rarity,
                description,
                safeImageUrls
            )
        );
        result.put("tips", buildTips(brand, condition, description, imageCount));
        result.put("imageCount", imageCount);
        return result;
    }

    @Transactional
    public AiEstimateTaskVO createTask(AiEstimateTaskRequest request) {
        AiEstimate estimate = new AiEstimate();
        estimate.setGoodsId(request.getGoodsId());
        estimate.setStatus("PROCESSING");
        estimate.setProvider("hybrid");
        estimate.setCreatedAt(LocalDateTime.now());
        estimate.setUpdatedAt(LocalDateTime.now());
        estimate = aiEstimateRepository.save(estimate);

        try {
            Map<String, Object> result = estimate(
                request.getTitle(),
                request.getCategory(),
                request.getBrand(),
                request.getCondition(),
                request.getDescription(),
                request.getImageUrls(),
                request.getYearsUsed(),
                request.getRarity()
            );
            applyEstimateResult(estimate, result);
            estimate.setStatus("DONE");
            estimate.setUpdatedAt(LocalDateTime.now());
            AiEstimate saved = aiEstimateRepository.save(estimate);
            syncGoodsEstimate(saved);
            return toVO(saved);
        } catch (Exception exception) {
            estimate.setStatus("FAILED");
            estimate.setSummary("Estimate failed");
            estimate.setRawResponse(exception.getMessage());
            estimate.setUpdatedAt(LocalDateTime.now());
            return toVO(aiEstimateRepository.save(estimate));
        }
    }

    public AiEstimateTaskVO getTask(Long estimateId) {
        return toVO(aiEstimateRepository.findById(estimateId)
            .orElseThrow(() -> new BusinessException("Estimate task not found")));
    }

    public AiEstimateTaskVO getLatestTaskForGoods(Long goodsId) {
        AiEstimate estimate = aiEstimateRepository.findFirstByGoodsIdAndStatusOrderByUpdatedAtDescIdDesc(goodsId, "DONE")
            .orElseGet(() -> aiEstimateRepository.findAllByGoodsIdOrderByUpdatedAtDescIdDesc(goodsId).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException("Estimate task not found")));
        return toVO(estimate);
    }

    private void applyEstimateResult(AiEstimate estimate, Map<String, Object> result) {
        estimate.setEstimatePrice(asBigDecimal(result.get("estimatePrice")));
        estimate.setPriceMin(asBigDecimal(result.get("lowPrice")));
        estimate.setPriceMax(asBigDecimal(result.get("highPrice")));
        estimate.setConfidence(asInteger(result.get("confidence")));
        estimate.setCarbonSavedKg(asInteger(result.get("carbonSavedKg")));
        estimate.setSummary(result.get("summary") == null ? null : String.valueOf(result.get("summary")));
        estimate.setRawResponse(result.toString());
    }

    private void syncGoodsEstimate(AiEstimate estimate) {
        if (estimate.getGoodsId() == null) {
            return;
        }
        goodsRepository.findById(estimate.getGoodsId()).ifPresent(goods -> {
            goods.setAiPrice(estimate.getEstimatePrice());
            goods.setCarbonSavedKg(estimate.getCarbonSavedKg());
            goodsRepository.save(goods);
        });
    }

    private AiEstimateTaskVO toVO(AiEstimate estimate) {
        return AiEstimateTaskVO.builder()
            .id(estimate.getId())
            .goodsId(estimate.getGoodsId())
            .status(estimate.getStatus())
            .provider(estimate.getProvider())
            .estimatePrice(estimate.getEstimatePrice())
            .priceMin(estimate.getPriceMin())
            .priceMax(estimate.getPriceMax())
            .confidence(estimate.getConfidence())
            .carbonSavedKg(estimate.getCarbonSavedKg())
            .summary(estimate.getSummary())
            .rawResponse(estimate.getRawResponse())
            .createdAt(estimate.getCreatedAt())
            .updatedAt(estimate.getUpdatedAt())
            .build();
    }

    private BigDecimal resolveBrandFactor(String brand) {
        if (brand == null || brand.isBlank()) {
            return BigDecimal.ONE;
        }
        String normalized = brand.trim().toLowerCase();
        if (normalized.contains("rolex") || normalized.contains("chanel") || normalized.contains("hermes")
            || normalized.contains("hermès") || normalized.contains("lv") || normalized.contains("dior")) {
            return new BigDecimal("1.18");
        }
        if (normalized.contains("leica") || normalized.contains("cartier") || normalized.contains("omega")
            || normalized.contains("gucci") || normalized.contains("celine")) {
            return new BigDecimal("1.10");
        }
        return new BigDecimal("1.03");
    }

    private BigDecimal resolveConditionFactor(String condition) {
        if (condition == null || condition.isBlank()) {
            return BigDecimal.ONE;
        }
        return switch (condition.trim()) {
            case "99 新" -> new BigDecimal("1.10");
            case "95 新" -> BigDecimal.ONE;
            case "90 新" -> new BigDecimal("0.90");
            case "85 新" -> new BigDecimal("0.80");
            default -> new BigDecimal("0.94");
        };
    }

    private BigDecimal resolveImageFactor(int imageCount) {
        return BigDecimal.valueOf(1 + Math.min(imageCount, 4) * 0.018d);
    }

    private BigDecimal resolveDescriptionFactor(String description) {
        if (description == null || description.isBlank()) {
            return BigDecimal.ONE;
        }
        int length = description.trim().length();
        return BigDecimal.valueOf(Math.min(1.06d, 1 + length / 250d));
    }

    private int resolveConfidence(int yearsUsed,
                                  int rarity,
                                  String condition,
                                  String description,
                                  int imageCount) {
        int confidence = 70;
        confidence += Math.min(14, rarity * 4);
        confidence -= Math.min(8, yearsUsed);
        if (condition != null && !condition.isBlank()) {
            confidence += 4;
        }
        if (description != null && !description.isBlank()) {
            confidence += Math.min(6, description.trim().length() / 18);
        }
        confidence += Math.min(12, imageCount * 3);
        return Math.max(72, Math.min(98, confidence));
    }

    private List<String> buildTips(String brand,
                                   String condition,
                                   String description,
                                   int imageCount) {
        List<String> tips = new ArrayList<>();
        if (imageCount < 2) {
            tips.add("补充正反面与细节近景图片，可显著提升估价置信度。");
        } else {
            tips.add("继续补充附件、编号或保卡特写，有助于缩小价格区间。");
        }
        if (description == null || description.trim().length() < 20) {
            tips.add("补充购入时间、使用频率和附件信息，AI 会给出更稳定的区间。");
        } else {
            tips.add("描述信息较完整，建议同步突出保卡、发票和维修记录。");
        }
        if (brand != null && !brand.isBlank()) {
            tips.add("高客单品牌建议叠加视频鉴定或到店复核，通常更利于成交。");
        } else if (condition == null || condition.isBlank()) {
            tips.add("补充成色等级后再估价，可减少价格波动。");
        } else {
            tips.add("完善品牌与成色标签后，可进一步优化推荐曝光。");
        }
        return tips;
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value == null) {
            return null;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private Integer asInteger(Object value) {
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value == null) {
            return null;
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
