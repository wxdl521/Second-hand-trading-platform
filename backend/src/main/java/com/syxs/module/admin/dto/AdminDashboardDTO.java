package com.syxs.module.admin.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminDashboardDTO {

    private BigDecimal totalGmv;
    private Integer newUsersCount;
    private Integer totalCarbonSaved;
    private Integer pendingGoodsCount;
    private Integer pendingKycCount;
    private Integer completedOrdersCount;
    private Integer auditApprovalRate;
    private Integer kycPassRate;
    private Integer orderCompletionRate;
    private List<AdminTrendPointDTO> recentGmv;
    private List<AdminChartItemDTO> orderFunnel;
    private List<AdminChartItemDTO> goodsAudit;
    private List<AdminChartItemDTO> userStatus;
    private List<AdminChartItemDTO> orderStatus;
    private List<AdminGoodsDTO> pendingGoods;
    private List<AdminUserDTO> pendingUsers;
}
