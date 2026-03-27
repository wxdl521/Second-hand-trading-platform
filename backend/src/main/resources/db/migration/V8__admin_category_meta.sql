ALTER TABLE category
    ADD COLUMN IF NOT EXISTS description VARCHAR(255) DEFAULT '',
    ADD COLUMN IF NOT EXISTS featured_brands VARCHAR(255) DEFAULT '',
    ADD COLUMN IF NOT EXISTS cover_image VARCHAR(255) DEFAULT '';

UPDATE category
SET description = CASE name
        WHEN '奢品箱包' THEN '聚焦高价值箱包、皮具与经典保值款商品的后台运营。'
        WHEN '珠宝腕表' THEN '统一管理珠宝首饰、腕表库存、估价与成交节奏。'
        WHEN '数码设备' THEN '覆盖相机、电脑与高端数码产品的循环交易品类。'
        WHEN '艺术收藏' THEN '用于管理收藏级艺术周边、配饰与稀缺性商品。'
        WHEN '家居好物' THEN '维护设计家具、家居摆件和生活方式商品的分类运营。'
        ELSE '用于管理后台商品分类与品牌标签。'
    END,
    featured_brands = CASE name
        WHEN '奢品箱包' THEN 'Chanel,Louis Vuitton,Hermès'
        WHEN '珠宝腕表' THEN 'Rolex,Cartier,Omega'
        WHEN '数码设备' THEN 'Leica,Apple,Sony'
        WHEN '艺术收藏' THEN 'Bearbrick,KAWS,Medicom Toy'
        WHEN '家居好物' THEN 'Vitra,Herman Miller,Muji'
        ELSE ''
    END,
    cover_image = CASE name
        WHEN '奢品箱包' THEN '/uploads/demo-chanel.svg'
        WHEN '珠宝腕表' THEN '/uploads/demo-rolex.svg'
        WHEN '数码设备' THEN '/uploads/demo-leica.svg'
        ELSE '/uploads/demo-created.svg'
    END
WHERE COALESCE(description, '') = '' OR COALESCE(featured_brands, '') = '' OR COALESCE(cover_image, '') = '';
