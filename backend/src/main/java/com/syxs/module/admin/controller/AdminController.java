package com.syxs.module.admin.controller;

import com.syxs.common.exception.BusinessException;
import com.syxs.common.result.R;
import com.syxs.common.utils.JwtUtil;
import com.syxs.module.admin.dto.AdminCategoryDTO;
import com.syxs.module.admin.dto.AdminCategoryUpsertRequest;
import com.syxs.module.admin.dto.AdminDashboardDTO;
import com.syxs.module.admin.dto.AdminGoodsDTO;
import com.syxs.module.admin.dto.AdminGoodsUpsertRequest;
import com.syxs.module.admin.dto.AdminOrderDTO;
import com.syxs.module.admin.dto.AdminOrderUpsertRequest;
import com.syxs.module.admin.dto.AdminReviewRequest;
import com.syxs.module.admin.dto.AdminUserDTO;
import com.syxs.module.admin.dto.AdminUserUpsertRequest;
import com.syxs.module.admin.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final JwtUtil jwtUtil;

    public AdminController(AdminService adminService, JwtUtil jwtUtil) {
        this.adminService = adminService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/dashboard")
    public R<AdminDashboardDTO> dashboard(HttpServletRequest request) {
        return R.ok(adminService.getDashboard(resolvePhone(request)));
    }

    @GetMapping("/goods")
    public R<List<AdminGoodsDTO>> goods(HttpServletRequest request) {
        return R.ok(adminService.listGoods(resolvePhone(request)));
    }

    @PostMapping("/goods")
    public R<AdminGoodsDTO> createGoods(HttpServletRequest request, @RequestBody AdminGoodsUpsertRequest body) {
        return R.ok(adminService.createGoods(resolvePhone(request), body));
    }

    @PutMapping("/goods/{id}")
    public R<AdminGoodsDTO> updateGoods(HttpServletRequest request,
                                        @PathVariable Long id,
                                        @RequestBody AdminGoodsUpsertRequest body) {
        return R.ok(adminService.updateGoods(id, resolvePhone(request), body));
    }

    @DeleteMapping("/goods/{id}")
    public R<Boolean> deleteGoods(HttpServletRequest request, @PathVariable Long id) {
        adminService.deleteGoods(id, resolvePhone(request));
        return R.ok(true);
    }

    @PostMapping("/goods/{id}/approve")
    public R<AdminGoodsDTO> approveGoods(HttpServletRequest request,
                                         @PathVariable Long id,
                                         @RequestBody(required = false) AdminReviewRequest body) {
        return R.ok(adminService.approveGoods(id, resolvePhone(request), body == null ? null : body.getReviewNote()));
    }

    @PostMapping("/goods/{id}/reject")
    public R<AdminGoodsDTO> rejectGoods(HttpServletRequest request,
                                        @PathVariable Long id,
                                        @RequestBody(required = false) AdminReviewRequest body) {
        return R.ok(adminService.rejectGoods(id, resolvePhone(request), body == null ? null : body.getReviewNote()));
    }

    @GetMapping("/users")
    public R<List<AdminUserDTO>> users(HttpServletRequest request) {
        return R.ok(adminService.listUsers(resolvePhone(request)));
    }

    @PostMapping("/users")
    public R<AdminUserDTO> createUser(HttpServletRequest request, @RequestBody AdminUserUpsertRequest body) {
        return R.ok(adminService.createUser(resolvePhone(request), body));
    }

    @PutMapping("/users/{id}")
    public R<AdminUserDTO> updateUser(HttpServletRequest request,
                                      @PathVariable Long id,
                                      @RequestBody AdminUserUpsertRequest body) {
        return R.ok(adminService.updateUser(id, resolvePhone(request), body));
    }

    @DeleteMapping("/users/{id}")
    public R<Boolean> deleteUser(HttpServletRequest request, @PathVariable Long id) {
        adminService.deleteUser(id, resolvePhone(request));
        return R.ok(true);
    }

    @PostMapping("/users/{id}/kyc/approve")
    public R<AdminUserDTO> approveUserKyc(HttpServletRequest request, @PathVariable Long id) {
        return R.ok(adminService.approveUserKyc(id, resolvePhone(request)));
    }

    @PostMapping("/users/{id}/kyc/reject")
    public R<AdminUserDTO> rejectUserKyc(HttpServletRequest request, @PathVariable Long id) {
        return R.ok(adminService.rejectUserKyc(id, resolvePhone(request)));
    }

    @PostMapping("/users/{id}/ban")
    public R<AdminUserDTO> banUser(HttpServletRequest request, @PathVariable Long id) {
        return R.ok(adminService.banUser(id, resolvePhone(request)));
    }

    @PostMapping("/users/{id}/unban")
    public R<AdminUserDTO> unbanUser(HttpServletRequest request, @PathVariable Long id) {
        return R.ok(adminService.unbanUser(id, resolvePhone(request)));
    }

    @GetMapping("/orders")
    public R<List<AdminOrderDTO>> orders(HttpServletRequest request) {
        return R.ok(adminService.listOrders(resolvePhone(request)));
    }

    @PostMapping("/orders")
    public R<AdminOrderDTO> createOrder(HttpServletRequest request, @RequestBody AdminOrderUpsertRequest body) {
        return R.ok(adminService.createOrder(resolvePhone(request), body));
    }

    @PutMapping("/orders/{id}")
    public R<AdminOrderDTO> updateOrder(HttpServletRequest request,
                                        @PathVariable Long id,
                                        @RequestBody AdminOrderUpsertRequest body) {
        return R.ok(adminService.updateOrder(id, resolvePhone(request), body));
    }

    @DeleteMapping("/orders/{id}")
    public R<Boolean> deleteOrder(HttpServletRequest request, @PathVariable Long id) {
        adminService.deleteOrder(id, resolvePhone(request));
        return R.ok(true);
    }

    @GetMapping("/categories")
    public R<List<AdminCategoryDTO>> categories(HttpServletRequest request) {
        return R.ok(adminService.listCategories(resolvePhone(request)));
    }

    @PostMapping("/categories")
    public R<AdminCategoryDTO> createCategory(HttpServletRequest request, @RequestBody AdminCategoryUpsertRequest body) {
        return R.ok(adminService.createCategory(resolvePhone(request), body));
    }

    @PutMapping("/categories/{id}")
    public R<AdminCategoryDTO> updateCategory(HttpServletRequest request,
                                              @PathVariable Long id,
                                              @RequestBody AdminCategoryUpsertRequest body) {
        return R.ok(adminService.updateCategory(id, resolvePhone(request), body));
    }

    @DeleteMapping("/categories/{id}")
    public R<Boolean> deleteCategory(HttpServletRequest request, @PathVariable Long id) {
        adminService.deleteCategory(id, resolvePhone(request));
        return R.ok(true);
    }

    private String resolvePhone(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
          String phone = authentication.getName();
          if (phone != null && !phone.isBlank() && !"anonymousUser".equals(phone)) {
              return phone;
          }
        }

        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return jwtUtil.parseSubject(authorization.substring(7));
        }

        throw new BusinessException("Unauthorized");
    }
}
