-- Keep terminal requests permanently: deleting them would permit replay of old payments.
CREATE TABLE admin_transaction_request (
    operator_id BIGINT NOT NULL,
    request_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    payload_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    user_id BIGINT NOT NULL,
    state VARCHAR(16) NOT NULL,
    message VARCHAR(255) NOT NULL DEFAULT '',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    completed_at DATETIME(3) NULL,
    PRIMARY KEY (operator_id, request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
