package com.syxs.module.carbon.repository;

import com.syxs.module.carbon.entity.CarbonAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarbonAccountRepository extends JpaRepository<CarbonAccount, Long> {

    Optional<CarbonAccount> findByUserId(Long userId);
}
