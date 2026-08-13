-- Optional wallet proof image used by the H5 Bind Wallet form.
ALTER TABLE goods_withdrawal_account
    ADD COLUMN attachment VARCHAR(1000) NULL AFTER wallet_address;
