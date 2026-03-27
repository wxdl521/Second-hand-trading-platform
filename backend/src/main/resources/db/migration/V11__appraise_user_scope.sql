ALTER TABLE appraise_order
    ADD COLUMN IF NOT EXISTS user_phone VARCHAR(32),
    ADD COLUMN IF NOT EXISTS created_at DATETIME NULL,
    ADD COLUMN IF NOT EXISTS updated_at DATETIME NULL;

UPDATE appraise_order
SET user_phone = COALESCE(user_phone, '13800000001'),
    created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, created_at, NOW());
