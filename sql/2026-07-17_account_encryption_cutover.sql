-- Phase 2. The application must already be running with ACCOUNT_SECURITY_MODE=ENCRYPTED_ONLY.
DROP PROCEDURE IF EXISTS verify_account_encryption_cutover;
DELIMITER //
CREATE PROCEDURE verify_account_encryption_cutover()
BEGIN
    DECLARE missing_ciphertext BIGINT DEFAULT 0;
    SELECT COUNT(*) INTO missing_ciphertext
    FROM goods_withdrawal_account
    WHERE (bank_account IS NOT NULL AND bank_account_enc IS NULL)
       OR (account_holder IS NOT NULL AND account_holder_enc IS NULL)
       OR (account_name IS NOT NULL AND account_name_enc IS NULL)
       OR (wallet_address IS NOT NULL AND wallet_address_enc IS NULL);

    IF missing_ciphertext > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Account encryption cutover refused: ciphertext backfill is incomplete';
    END IF;
END//
DELIMITER ;

CALL verify_account_encryption_cutover();
DROP PROCEDURE verify_account_encryption_cutover;

UPDATE goods_withdrawal_account
SET bank_account = NULL,
    account_holder = NULL,
    account_name = NULL,
    wallet_address = NULL,
    update_time = NOW(3)
WHERE bank_account_enc IS NOT NULL
   OR account_holder_enc IS NOT NULL
   OR account_name_enc IS NOT NULL
   OR wallet_address_enc IS NOT NULL;

SELECT COUNT(*) AS remaining_plaintext
FROM goods_withdrawal_account
WHERE bank_account IS NOT NULL
   OR account_holder IS NOT NULL
   OR account_name IS NOT NULL
   OR wallet_address IS NOT NULL;
