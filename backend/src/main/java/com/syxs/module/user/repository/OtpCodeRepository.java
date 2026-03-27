package com.syxs.module.user.repository;

import com.syxs.module.user.entity.OtpCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findTopByPhoneOrderByCreatedAtDesc(String phone);
}
