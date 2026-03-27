package com.syxs.module.appraise.service;

import com.syxs.module.appraise.entity.AppraiseOrder;
import java.util.List;

public interface AppraiseService {

    List<AppraiseOrder> list(String userPhone);

    AppraiseOrder create(AppraiseOrder request, String userPhone);
}
