package com.syxs.module.carbon.service;

import com.syxs.module.carbon.entity.CarbonRecord;
import java.util.List;
import java.util.Map;

public interface CarbonService {

    Map<String, Object> summary(String operatorPhone);

    Map<String, Object> account(String operatorPhone);

    List<CarbonRecord> records(String operatorPhone);

    byte[] certificate(String operatorPhone);
}
