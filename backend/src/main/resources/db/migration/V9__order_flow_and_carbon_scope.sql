ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS buyer_phone VARCHAR(32) NULL,
    ADD COLUMN IF NOT EXISTS paid_at DATETIME NULL,
    ADD COLUMN IF NOT EXISTS shipped_at DATETIME NULL,
    ADD COLUMN IF NOT EXISTS completed_at DATETIME NULL;

UPDATE orders
SET buyer_phone = '13800000001'
WHERE buyer_phone IS NULL
  AND buyer_name IS NOT NULL;

UPDATE orders
SET paid_at = COALESCE(paid_at, created_at)
WHERE status IN ('PENDING_SHIPMENT', 'IN_TRANSIT', 'COMPLETED');

UPDATE orders
SET shipped_at = COALESCE(shipped_at, paid_at, created_at)
WHERE status IN ('IN_TRANSIT', 'COMPLETED');

UPDATE orders
SET completed_at = COALESCE(completed_at, shipped_at, paid_at, created_at)
WHERE status = 'COMPLETED';

ALTER TABLE carbon_record
    ADD COLUMN IF NOT EXISTS user_id BIGINT NULL;

UPDATE carbon_record
SET user_id = (
    SELECT id FROM users WHERE phone = '13800000001' LIMIT 1
)
WHERE user_id IS NULL;
