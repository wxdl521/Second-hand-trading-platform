# 数据命名映射说明（PRD vs 当前物理表）

> 本文档仅用于说明命名映射，不包含任何数据库变更逻辑。

## 背景

PRD 文档中使用了 `t_*` 前缀命名（如 `t_user`、`t_goods`）。
当前本地开发版数据库采用简化物理表名（如 `users`、`goods`）。

为避免在 Flyway 旧迁移文件中追加说明性注释造成 checksum 风险，映射关系统一记录在此文档。

## 映射关系

- `t_user` -> `users`
- `t_goods` -> `goods`
- `t_goods_image` -> `goods_image`
- `t_order` -> `orders`
- `t_appraise_order` -> `appraise_order`
- `t_carbon_account` -> `carbon_account`
- `t_carbon_record` -> `carbon_record`
- `t_category` -> `category`
- `t_brand` -> `brand`
- `t_ai_estimate` -> `ai_estimate`
- `t_favorite` -> `favorite`
- `t_message` -> `message`

## 约束

- 已发布的 Flyway 迁移文件不做内容变更。
- 若后续需补充映射，请优先更新本文档或新增独立文档。
