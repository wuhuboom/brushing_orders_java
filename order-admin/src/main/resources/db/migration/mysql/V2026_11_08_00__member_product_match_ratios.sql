-- NULL preserves the historical behavior of members whose product-matching
-- switch was saved before personal matching ratios were supported.
-- Do not backfill from membership levels: that would activate a new price rule.
ALTER TABLE order_user
    ADD COLUMN product_match_min DECIMAL(5,2) NULL DEFAULT NULL COMMENT '个人产品匹配最小比例（百分比）',
    ADD COLUMN product_match_max DECIMAL(5,2) NULL DEFAULT NULL COMMENT '个人产品匹配最大比例（百分比）';
