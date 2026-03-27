ALTER TABLE goods
    ADD COLUMN IF NOT EXISTS audit_status VARCHAR(32) DEFAULT '审核通过',
    ADD COLUMN IF NOT EXISTS review_note VARCHAR(255) DEFAULT '审核通过，允许正常流转。',
    ADD COLUMN IF NOT EXISTS transfer_type VARCHAR(32) DEFAULT '自卖',
    ADD COLUMN IF NOT EXISTS view_count INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS favor_count INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS mock_certified BIT DEFAULT 0;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS account_status VARCHAR(32) DEFAULT '正常',
    ADD COLUMN IF NOT EXISTS kyc_review_status VARCHAR(32) DEFAULT '已通过',
    ADD COLUMN IF NOT EXISTS last_active_at DATETIME NULL;

UPDATE goods
SET audit_status = '待审核',
    review_note = '新发布商品，待运营审核。',
    transfer_type = '自卖',
    view_count = 320,
    favor_count = 18,
    mock_certified = b'0'
WHERE title = '香奈儿 Classic Flap 中号链条包';

UPDATE goods
SET audit_status = '审核通过',
    review_note = '审核通过，允许上架流转。',
    transfer_type = '寄卖',
    view_count = 580,
    favor_count = 42,
    mock_certified = b'1'
WHERE title = '劳力士 Datejust 36 蓝盘腕表';

UPDATE goods
SET audit_status = '已驳回',
    review_note = '细节图与附件说明不足，请补充后重新提交。',
    transfer_type = '极速回收',
    view_count = 240,
    favor_count = 12,
    mock_certified = b'0'
WHERE title = '徕卡 Q3 全画幅相机套装';

UPDATE users
SET account_status = '正常',
    kyc_review_status = CASE
        WHEN phone = '13800000001' THEN '待审核'
        ELSE '已通过'
    END,
    last_active_at = COALESCE(last_active_at, created_at, NOW());
