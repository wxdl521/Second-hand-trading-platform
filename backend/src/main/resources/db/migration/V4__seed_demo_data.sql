INSERT IGNORE INTO users (nickname, phone, password, avatar, city, bio, role, kyc_level, carbon_points, created_at)
VALUES ('林知夏', '13800000001', '{noop}Test@123', '/uploads/default-avatar.svg', '上海', '关注循环时尚与高质感生活方式。', 'USER', 'L1', 1280, NOW());

INSERT IGNORE INTO users (nickname, phone, password, avatar, city, bio, role, kyc_level, carbon_points, created_at)
VALUES ('宋屿', '13800000002', '{noop}Test@123', '/uploads/default-avatar.svg', '杭州', '专注高端数码与奢侈品循环交易。', 'SELLER', 'L3', 2560, NOW());

INSERT IGNORE INTO users (nickname, phone, password, avatar, city, bio, role, kyc_level, carbon_points, created_at)
VALUES ('程砚', '13800000003', '{noop}Test@123', '/uploads/default-avatar.svg', '北京', '负责高价值商品的鉴定与复核。', 'APPRAISER', 'L3', 1880, NOW());

INSERT IGNORE INTO users (nickname, phone, password, avatar, city, bio, role, kyc_level, carbon_points, created_at)
VALUES ('平台管理员', '13800000099', '{noop}Admin@123', '/uploads/default-avatar.svg', '上海', '负责平台审核、用户运营与内容巡检。', 'ADMIN', 'L3', 3000, NOW());

INSERT INTO goods (title, category, brand, condition_level, sale_price, original_price, ai_price, carbon_saved_kg, city, seller_name, seller_level, cover_url, story, tags, description, status, created_at)
SELECT '香奈儿 Classic Flap 中号链条包', '奢品箱包', 'Chanel', '95 新', 27800.00, 58999.00, 28100.00, 36, '上海', '苏黎', 'L3', '/uploads/demo-chanel.svg', '陪伴过两次晚宴，保养得很好，五金成色优秀。', '经典款,全套附件,保值款', '附原盒、防尘袋、购入小票，支持视频验货与线下复核。', 'ON_SALE', NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM goods WHERE title = '香奈儿 Classic Flap 中号链条包');

INSERT INTO goods (title, category, brand, condition_level, sale_price, original_price, ai_price, carbon_saved_kg, city, seller_name, seller_level, cover_url, story, tags, description, status, created_at)
SELECT '劳力士 Datejust 36 蓝盘腕表', '珠宝腕表', 'Rolex', '98 新', 56800.00, 75999.00, 55200.00, 28, '深圳', '周衡', 'L3', '/uploads/demo-rolex.svg', '2024 年购入，仅日常通勤佩戴，走时稳定。', '蓝盘,保卡齐全,支持寄售', '支持平台寄售与鉴定预约，附保卡和原装表节。', 'ON_SALE', NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM goods WHERE title = '劳力士 Datejust 36 蓝盘腕表');

INSERT INTO goods (title, category, brand, condition_level, sale_price, original_price, ai_price, carbon_saved_kg, city, seller_name, seller_level, cover_url, story, tags, description, status, created_at)
SELECT '徕卡 Q3 全画幅相机套装', '数码设备', 'Leica', '92 新', 31800.00, 46888.00, 32600.00, 42, '北京', '江沅', 'L2', '/uploads/demo-leica.svg', '主要用于旅行拍摄，快门次数很低。', '低快门,摄影好物,配件齐', '带手柄与备用电池，平台 AI 估价波动较小。', 'ON_SALE', NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM goods WHERE title = '徕卡 Q3 全画幅相机套装');

INSERT INTO goods_image (goods_id, image_url, sort_no, is_cover)
SELECT g.id, '/uploads/demo-chanel.svg', 1, 1
FROM goods g
WHERE g.title = '香奈儿 Classic Flap 中号链条包'
  AND NOT EXISTS (
    SELECT 1 FROM goods_image gi
    WHERE gi.goods_id = g.id AND gi.image_url = '/uploads/demo-chanel.svg'
  );

INSERT INTO goods_image (goods_id, image_url, sort_no, is_cover)
SELECT g.id, '/uploads/demo-chanel-detail.svg', 2, 0
FROM goods g
WHERE g.title = '香奈儿 Classic Flap 中号链条包'
  AND NOT EXISTS (
    SELECT 1 FROM goods_image gi
    WHERE gi.goods_id = g.id AND gi.image_url = '/uploads/demo-chanel-detail.svg'
  );

INSERT INTO goods_image (goods_id, image_url, sort_no, is_cover)
SELECT g.id, '/uploads/demo-rolex.svg', 1, 1
FROM goods g
WHERE g.title = '劳力士 Datejust 36 蓝盘腕表'
  AND NOT EXISTS (
    SELECT 1 FROM goods_image gi
    WHERE gi.goods_id = g.id AND gi.image_url = '/uploads/demo-rolex.svg'
  );

INSERT INTO goods_image (goods_id, image_url, sort_no, is_cover)
SELECT g.id, '/uploads/demo-rolex-detail.svg', 2, 0
FROM goods g
WHERE g.title = '劳力士 Datejust 36 蓝盘腕表'
  AND NOT EXISTS (
    SELECT 1 FROM goods_image gi
    WHERE gi.goods_id = g.id AND gi.image_url = '/uploads/demo-rolex-detail.svg'
  );

INSERT INTO goods_image (goods_id, image_url, sort_no, is_cover)
SELECT g.id, '/uploads/demo-leica.svg', 1, 1
FROM goods g
WHERE g.title = '徕卡 Q3 全画幅相机套装'
  AND NOT EXISTS (
    SELECT 1 FROM goods_image gi
    WHERE gi.goods_id = g.id AND gi.image_url = '/uploads/demo-leica.svg'
  );

INSERT INTO goods_image (goods_id, image_url, sort_no, is_cover)
SELECT g.id, '/uploads/demo-leica-detail.svg', 2, 0
FROM goods g
WHERE g.title = '徕卡 Q3 全画幅相机套装'
  AND NOT EXISTS (
    SELECT 1 FROM goods_image gi
    WHERE gi.goods_id = g.id AND gi.image_url = '/uploads/demo-leica-detail.svg'
  );

INSERT INTO orders (goods_id, buyer_name, amount, status, created_at)
SELECT g.id, '林知夏', 27800.00, 'PENDING_PAYMENT', NOW()
FROM goods g
WHERE g.title = '香奈儿 Classic Flap 中号链条包'
  AND NOT EXISTS (
    SELECT 1 FROM orders o
    WHERE o.goods_id = g.id AND o.buyer_name = '林知夏' AND o.amount = 27800.00
  );

INSERT INTO orders (goods_id, buyer_name, amount, status, created_at)
SELECT g.id, '林知夏', 31800.00, 'IN_TRANSIT', NOW()
FROM goods g
WHERE g.title = '徕卡 Q3 全画幅相机套装'
  AND NOT EXISTS (
    SELECT 1 FROM orders o
    WHERE o.goods_id = g.id AND o.buyer_name = '林知夏' AND o.amount = 31800.00
  );

INSERT INTO appraise_order (goods_title, mode, booking_time, note, status)
SELECT '香奈儿 Classic Flap 中号链条包', '视频连线鉴定', '2026-03-25 19:30', '重点确认五金磨损与内里编码。', 'CONFIRMED'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM appraise_order
  WHERE goods_title = '香奈儿 Classic Flap 中号链条包' AND booking_time = '2026-03-25 19:30'
);

INSERT INTO carbon_account (user_id, balance)
SELECT u.id, 1445
FROM users u
WHERE u.phone = '13800000001'
  AND NOT EXISTS (
    SELECT 1 FROM carbon_account ca WHERE ca.user_id = u.id
  );

INSERT INTO carbon_record (title, points, type, biz_date, description)
SELECT '完成闲置交易', 180, '收入', '2026-03-20', '完成一笔高价值循环交易，累计减碳 12kg。'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM carbon_record
  WHERE title = '完成闲置交易' AND biz_date = '2026-03-20'
);

INSERT INTO carbon_record (title, points, type, biz_date, description)
SELECT 'AI 估价任务', 45, '收入', '2026-03-21', '提交 3 次有效估价请求，获得探索积分。'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM carbon_record
  WHERE title = 'AI 估价任务' AND biz_date = '2026-03-21'
);

INSERT INTO carbon_record (title, points, type, biz_date, description)
SELECT '兑换顺丰保价券', 60, '支出', '2026-03-22', '用于高价值寄售商品的物流权益。'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM carbon_record
  WHERE title = '兑换顺丰保价券' AND biz_date = '2026-03-22'
);
