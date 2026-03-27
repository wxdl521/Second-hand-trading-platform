package com.syxs.module.admin.service;

import com.syxs.module.admin.dto.AdminLoginRequest;
import com.syxs.module.admin.dto.AdminDashboardDTO;
import com.syxs.module.admin.dto.AdminCategoryDTO;
import com.syxs.module.admin.dto.AdminCategoryUpsertRequest;
import com.syxs.module.admin.dto.AdminGoodsDTO;
import com.syxs.module.admin.dto.AdminGoodsUpsertRequest;
import com.syxs.module.admin.dto.AdminOrderDTO;
import com.syxs.module.admin.dto.AdminOrderUpsertRequest;
import com.syxs.module.admin.dto.AdminUserDTO;
import com.syxs.module.admin.dto.AdminUserUpsertRequest;
import java.util.List;
import com.syxs.module.user.dto.UserProfileDTO;

public interface AdminService {

    UserProfileDTO login(AdminLoginRequest request);

    List<AdminUserDTO> listUsers(String operatorPhone);

    List<AdminGoodsDTO> listGoods(String operatorPhone);

    AdminGoodsDTO createGoods(String operatorPhone, AdminGoodsUpsertRequest request);

    AdminGoodsDTO updateGoods(Long id, String operatorPhone, AdminGoodsUpsertRequest request);

    void deleteGoods(Long id, String operatorPhone);

    AdminGoodsDTO approveGoods(Long id, String operatorPhone, String reviewNote);

    AdminGoodsDTO rejectGoods(Long id, String operatorPhone, String reviewNote);

    List<AdminOrderDTO> listOrders(String operatorPhone);

    AdminOrderDTO createOrder(String operatorPhone, AdminOrderUpsertRequest request);

    AdminOrderDTO updateOrder(Long id, String operatorPhone, AdminOrderUpsertRequest request);

    void deleteOrder(Long id, String operatorPhone);

    AdminDashboardDTO getDashboard(String operatorPhone);

    AdminUserDTO createUser(String operatorPhone, AdminUserUpsertRequest request);

    AdminUserDTO updateUser(Long id, String operatorPhone, AdminUserUpsertRequest request);

    void deleteUser(Long id, String operatorPhone);

    AdminUserDTO approveUserKyc(Long id, String operatorPhone);

    AdminUserDTO rejectUserKyc(Long id, String operatorPhone);

    AdminUserDTO banUser(Long id, String operatorPhone);

    AdminUserDTO unbanUser(Long id, String operatorPhone);

    List<AdminCategoryDTO> listCategories(String operatorPhone);

    AdminCategoryDTO createCategory(String operatorPhone, AdminCategoryUpsertRequest request);

    AdminCategoryDTO updateCategory(Long id, String operatorPhone, AdminCategoryUpsertRequest request);

    void deleteCategory(Long id, String operatorPhone);
}
