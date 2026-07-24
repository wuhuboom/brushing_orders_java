CREATE TABLE IF NOT EXISTS goods_backup_before_order_goods_sync AS
SELECT * FROM goods;

ALTER TABLE goods
    MODIFY COLUMN price decimal(20, 2) NOT NULL;

SET @target_type_id := (
    SELECT id
    FROM goods_type
    WHERE is_enabled = '0'
    ORDER BY id
    LIMIT 1
);

SET @target_type_id := COALESCE(
    @target_type_id,
    (SELECT id FROM goods_type ORDER BY id LIMIT 1),
    1
);

INSERT INTO goods (
    id,
    title,
    type_id,
    is_enabled,
    price,
    serial_number,
    image,
    description,
    create_time
)
SELECT
    og.id,
    LEFT(og.name, 300),
    @target_type_id,
    COALESCE(NULLIF(og.status, ''), '0'),
    COALESCE(og.price, 0),
    og.id,
    COALESCE(NULLIF(og.cover_url, ''), ''),
    og.description,
    og.create_time
FROM order_goods og
WHERE og.id IS NOT NULL
  AND og.name IS NOT NULL
  AND og.name <> ''
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    type_id = VALUES(type_id),
    is_enabled = VALUES(is_enabled),
    price = VALUES(price),
    image = CASE
        WHEN VALUES(image) <> '' THEN VALUES(image)
        ELSE goods.image
    END,
    description = CASE
        WHEN VALUES(description) IS NOT NULL THEN VALUES(description)
        ELSE goods.description
    END,
    create_time = CASE
        WHEN VALUES(create_time) IS NOT NULL THEN VALUES(create_time)
        ELSE goods.create_time
    END;

DROP TABLE IF EXISTS order_goods;
