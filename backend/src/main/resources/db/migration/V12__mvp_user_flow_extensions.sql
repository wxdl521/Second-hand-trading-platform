ALTER TABLE goods
    ADD COLUMN IF NOT EXISTS seller_phone VARCHAR(32) NULL;

UPDATE goods g
LEFT JOIN users u ON u.nickname = g.seller_name
SET g.seller_phone = COALESCE(g.seller_phone, u.phone, '13800000001')
WHERE g.seller_phone IS NULL;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS seller_name VARCHAR(100) NULL,
    ADD COLUMN IF NOT EXISTS seller_phone VARCHAR(32) NULL,
    ADD COLUMN IF NOT EXISTS refunded_at DATETIME NULL;

UPDATE orders o
JOIN goods g ON g.id = o.goods_id
SET o.seller_name = COALESCE(o.seller_name, g.seller_name),
    o.seller_phone = COALESCE(o.seller_phone, g.seller_phone)
WHERE o.seller_name IS NULL
   OR o.seller_phone IS NULL;

CREATE TABLE IF NOT EXISTS ai_estimate (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    goods_id BIGINT NULL,
    status VARCHAR(32) NOT NULL,
    provider VARCHAR(64),
    estimate_price DECIMAL(10, 2),
    price_min DECIMAL(10, 2),
    price_max DECIMAL(10, 2),
    confidence INT,
    carbon_saved_kg INT,
    summary VARCHAR(500),
    raw_response TEXT,
    created_at DATETIME NULL,
    updated_at DATETIME NULL
);

CREATE TABLE IF NOT EXISTS favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    goods_id BIGINT NOT NULL,
    created_at DATETIME NULL,
    UNIQUE KEY uk_favorite_user_goods (user_id, goods_id)
);

CREATE TABLE IF NOT EXISTS message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type VARCHAR(64),
    title VARCHAR(128),
    content VARCHAR(500),
    is_read BIT DEFAULT 0,
    created_at DATETIME NULL,
    read_at DATETIME NULL
);

UPDATE goods g
SET g.favor_count = (
    SELECT COUNT(*)
    FROM favorite f
    WHERE f.goods_id = g.id
)
WHERE g.favor_count IS NULL
   OR g.favor_count = 0;
