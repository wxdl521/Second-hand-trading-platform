package com.syxs.module.admin.repository;

import com.syxs.module.admin.entity.AdminCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminCategoryRepository extends JpaRepository<AdminCategory, Long> {

    List<AdminCategory> findAllByOrderBySortNoAscIdAsc();

    Optional<AdminCategory> findByName(String name);
}
