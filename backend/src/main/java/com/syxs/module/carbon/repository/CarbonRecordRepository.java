package com.syxs.module.carbon.repository;

import com.syxs.module.carbon.entity.CarbonRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarbonRecordRepository extends JpaRepository<CarbonRecord, Long> {

    List<CarbonRecord> findAllByUserIdOrderByBizDateDescIdDesc(Long userId);
}
