CREATE TABLE IF NOT EXISTS t_otp (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    phone VARCHAR(32) NOT NULL,
    code VARCHAR(16) NOT NULL,
    expires_at DATETIME NOT NULL,
    used BIT DEFAULT 0,
    created_at DATETIME NOT NULL
);

SET @idx_exists = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 't_otp'
      AND index_name = 'idx_t_otp_phone_created_at'
);

SET @idx_sql = IF(
    @idx_exists = 0,
    'CREATE INDEX idx_t_otp_phone_created_at ON t_otp (phone, created_at)',
    'SELECT 1'
);

PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
