
USE syxs_db;
CREATE TABLE IF NOT EXISTS category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL UNIQUE,
    sort_no INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS brand (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL UNIQUE,
    category_name VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nickname VARCHAR(100) NOT NULL,
    phone VARCHAR(32) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    avatar VARCHAR(255),
    city VARCHAR(64),
    bio VARCHAR(255),
    role VARCHAR(32),
    kyc_level VARCHAR(16),
    carbon_points INT DEFAULT 0,
    created_at DATETIME
);

CREATE TABLE IF NOT EXISTS goods (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(64) NOT NULL,
    brand VARCHAR(64) NOT NULL,
    condition_level VARCHAR(32),
    sale_price DECIMAL(10, 2),
    original_price DECIMAL(10, 2),
    ai_price DECIMAL(10, 2),
    carbon_saved_kg INT DEFAULT 0,
    city VARCHAR(64),
    seller_name VARCHAR(100),
    seller_level VARCHAR(16),
    cover_url VARCHAR(255),
    story VARCHAR(255),
    tags VARCHAR(255),
    description TEXT,
    status VARCHAR(32),
    created_at DATETIME
);

CREATE TABLE IF NOT EXISTS goods_image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    goods_id BIGINT NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    sort_no INT DEFAULT 0,
    is_cover BIT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    goods_id BIGINT NOT NULL,
    buyer_name VARCHAR(100),
    amount DECIMAL(10, 2),
    status VARCHAR(32),
    created_at DATETIME
);

CREATE TABLE IF NOT EXISTS appraise_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    goods_title VARCHAR(255),
    mode VARCHAR(64),
    booking_time VARCHAR(64),
    note VARCHAR(500),
    status VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS carbon_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    balance INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS carbon_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(128),
    points INT,
    type VARCHAR(16),
    biz_date VARCHAR(32),
    description VARCHAR(255)
);
