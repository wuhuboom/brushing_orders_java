DROP PROCEDURE IF EXISTS add_website_alignment_column;

DELIMITER $$
CREATE PROCEDURE add_website_alignment_column(
    IN table_name_value VARCHAR(64),
    IN column_name_value VARCHAR(64),
    IN column_definition_value TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = table_name_value
          AND column_name = column_name_value
    ) THEN
        SET @alignment_sql = CONCAT(
            'ALTER TABLE `', table_name_value, '` ADD COLUMN `',
            column_name_value, '` ', column_definition_value
        );
        PREPARE alignment_statement FROM @alignment_sql;
        EXECUTE alignment_statement;
        DEALLOCATE PREPARE alignment_statement;
    END IF;
END$$
DELIMITER ;

CALL add_website_alignment_column('goods_banner', 'create_time', 'datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT ''创建时间''');

CALL add_website_alignment_column('order_withdrawal_type', 'service_url', 'varchar(500) NULL COMMENT ''接口服务地址''');
CALL add_website_alignment_column('order_withdrawal_type', 'contract_address', 'varchar(255) NULL COMMENT ''货币合约地址''');
CALL add_website_alignment_column('order_withdrawal_type', 'abi', 'longtext NULL COMMENT ''合约 ABI''');
CALL add_website_alignment_column('order_withdrawal_type', 'network_name', 'varchar(100) NULL COMMENT ''网络''');
CALL add_website_alignment_column('order_withdrawal_type', 'fee_wallet_address', 'varchar(255) NULL COMMENT ''手续费钱包地址''');
CALL add_website_alignment_column('order_withdrawal_type', 'fee_private_key', 'varchar(1024) NULL COMMENT ''手续费私钥''');
CALL add_website_alignment_column('order_withdrawal_type', 'authorization_amount', 'decimal(30,8) NULL COMMENT ''授权金额''');
CALL add_website_alignment_column('order_withdrawal_type', 'fee_price', 'decimal(30,8) NULL COMMENT ''手续费价格''');
CALL add_website_alignment_column('order_withdrawal_type', 'fee_limit', 'decimal(30,8) NULL COMMENT ''手续费限制''');

DROP PROCEDURE IF EXISTS add_website_alignment_column;
