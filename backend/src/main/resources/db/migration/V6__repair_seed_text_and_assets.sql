DELETE FROM category WHERE name = '????';

INSERT IGNORE INTO category (name, sort_no) VALUES ('奢品箱包', 1);
INSERT IGNORE INTO category (name, sort_no) VALUES ('珠宝腕表', 2);
INSERT IGNORE INTO category (name, sort_no) VALUES ('数码设备', 3);
INSERT IGNORE INTO category (name, sort_no) VALUES ('艺术收藏', 4);
INSERT IGNORE INTO category (name, sort_no) VALUES ('家居好物', 5);

UPDATE brand SET category_name = '奢品箱包' WHERE name IN ('Chanel', 'Dior');
UPDATE brand SET category_name = '珠宝腕表' WHERE name = 'Rolex';
UPDATE brand SET category_name = '数码设备' WHERE name = 'Leica';
UPDATE brand SET category_name = '艺术收藏' WHERE name IN ('Herm?s', 'Hermès');
UPDATE brand SET category_name = '家居好物' WHERE name = 'Bang & Olufsen';
UPDATE brand SET name = 'Hermès' WHERE name = 'Herm?s';

INSERT IGNORE INTO brand (name, category_name) VALUES ('Chanel', '奢品箱包');
INSERT IGNORE INTO brand (name, category_name) VALUES ('Dior', '奢品箱包');
INSERT IGNORE INTO brand (name, category_name) VALUES ('Rolex', '珠宝腕表');
INSERT IGNORE INTO brand (name, category_name) VALUES ('Leica', '数码设备');
INSERT IGNORE INTO brand (name, category_name) VALUES ('Hermès', '艺术收藏');
INSERT IGNORE INTO brand (name, category_name) VALUES ('Bang & Olufsen', '家居好物');

UPDATE users
SET avatar = '/uploads/default-avatar.svg'
WHERE avatar = '/uploads/default-avatar.png';

UPDATE users
SET nickname = '林知夏', city = '上海', bio = '关注循环时尚与高质感生活方式。', avatar = '/uploads/default-avatar.svg'
WHERE phone = '13800000001';

UPDATE users
SET nickname = '宋屿', city = '杭州', bio = '专注高端数码与奢侈品循环交易。', avatar = '/uploads/default-avatar.svg'
WHERE phone = '13800000002';

UPDATE users
SET nickname = '程砚', city = '北京', bio = '负责高价值商品的鉴定与复核。', avatar = '/uploads/default-avatar.svg'
WHERE phone = '13800000003';

UPDATE users
SET nickname = '平台管理员', city = '上海', bio = '负责平台审核、用户运营与内容巡检。', avatar = '/uploads/default-avatar.svg'
WHERE phone = '13800000099';

UPDATE goods
SET title = '香奈儿 Classic Flap 中号链条包',
    category = '奢品箱包',
    condition_level = '95 新',
    city = '上海',
    seller_name = '苏黎',
    seller_level = 'L3',
    cover_url = '/uploads/demo-chanel.svg',
    story = '陪伴过两次晚宴，保养得很好，五金成色优秀。',
    tags = '经典款,全套附件,保值款',
    description = '附原盒、防尘袋、购入小票，支持视频验货与线下复核。'
WHERE brand = 'Chanel'
  AND (title LIKE '%Classic Flap%' OR cover_url IN ('/uploads/demo-chanel.png', '/uploads/demo-chanel.svg'));

UPDATE goods
SET title = '劳力士 Datejust 36 蓝盘腕表',
    category = '珠宝腕表',
    condition_level = '98 新',
    city = '深圳',
    seller_name = '周衡',
    seller_level = 'L3',
    cover_url = '/uploads/demo-rolex.svg',
    story = '2024 年购入，仅日常通勤佩戴，走时稳定。',
    tags = '蓝盘,保卡齐全,支持寄售',
    description = '支持平台寄售与鉴定预约，附保卡和原装表节。'
WHERE brand = 'Rolex'
  AND (title LIKE '%Datejust%' OR cover_url IN ('/uploads/demo-rolex.png', '/uploads/demo-rolex.svg'));

UPDATE goods
SET title = '徕卡 Q3 全画幅相机套装',
    category = '数码设备',
    condition_level = '92 新',
    city = '北京',
    seller_name = '江沅',
    seller_level = 'L2',
    cover_url = '/uploads/demo-leica.svg',
    story = '主要用于旅行拍摄，快门次数很低。',
    tags = '低快门,摄影好物,配件齐',
    description = '带手柄与备用电池，平台 AI 估价波动较小。'
WHERE brand = 'Leica'
  AND (title LIKE '%Q3%' OR cover_url IN ('/uploads/demo-leica.png', '/uploads/demo-leica.svg'));

UPDATE goods
SET cover_url = '/uploads/demo-created.svg'
WHERE cover_url = '/uploads/demo-created.png';

UPDATE goods_image SET image_url = '/uploads/demo-chanel.svg' WHERE image_url = '/uploads/demo-chanel.png';
UPDATE goods_image SET image_url = '/uploads/demo-chanel-detail.svg' WHERE image_url = '/uploads/demo-chanel-detail.png';
UPDATE goods_image SET image_url = '/uploads/demo-rolex.svg' WHERE image_url = '/uploads/demo-rolex.png';
UPDATE goods_image SET image_url = '/uploads/demo-rolex-detail.svg' WHERE image_url = '/uploads/demo-rolex-detail.png';
UPDATE goods_image SET image_url = '/uploads/demo-leica.svg' WHERE image_url = '/uploads/demo-leica.png';
UPDATE goods_image SET image_url = '/uploads/demo-leica-detail.svg' WHERE image_url = '/uploads/demo-leica-detail.png';
UPDATE goods_image SET image_url = '/uploads/demo-created.svg' WHERE image_url = '/uploads/demo-created.png';

UPDATE orders o
JOIN goods g ON g.id = o.goods_id
SET o.buyer_name = '林知夏'
WHERE g.brand IN ('Chanel', 'Leica')
  AND o.buyer_name = '????';

UPDATE appraise_order
SET goods_title = '香奈儿 Classic Flap 中号链条包',
    mode = '视频连线鉴定',
    note = '重点确认五金磨损与内里编码。'
WHERE goods_title = '??? Classic Flap ?????'
   OR goods_title LIKE '%Classic Flap%';

UPDATE carbon_record
SET title = '完成闲置交易',
    type = '收入',
    description = '完成一笔高价值循环交易，累计减碳 12kg。'
WHERE biz_date = '2026-03-20';

UPDATE carbon_record
SET title = 'AI 估价任务',
    type = '收入',
    description = '提交 3 次有效估价请求，获得探索积分。'
WHERE biz_date = '2026-03-21';

UPDATE carbon_record
SET title = '兑换顺丰保价券',
    type = '支出',
    description = '用于高价值寄售商品的物流权益。'
WHERE biz_date = '2026-03-22';
