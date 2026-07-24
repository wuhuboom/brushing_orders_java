# Account and withdrawal rollout

## Phase 1: dual write

1. Back up the database and run `2026-07-17_account_withdrawal_hardening.sql`.
2. Generate a 32-byte random key outside the repository. Configure:

   ```text
   ACCOUNT_SECURITY_MODE=DUAL_WRITE
   ACCOUNT_DATA_ACTIVE_KEY_ID=v1
   ACCOUNT_DATA_ENCRYPTION_KEYS=v1:<base64-32-byte-key>
   ACCOUNT_DATA_BACKFILL_ENABLED=true
   ACCOUNT_DATA_BACKFILL_DIRECTION=ENCRYPT
   ```

3. Start one application instance with backfill enabled. Monitor
   `withdrawal_account_encryption_backfill` until the completion event is logged.
4. Disable the backfill flag and restart normally. Verify that every populated legacy
   sensitive field has a ciphertext shadow and mask. Retain old keys in
   `ACCOUNT_DATA_ENCRYPTION_KEYS` when rotating the active key.

The backfill is resumable and idempotent: it only selects rows whose populated
plaintext field does not yet have a ciphertext counterpart.

## Phase 2: encrypted-only

1. Take a fresh backup and sample-decrypt records with the sensitive-data permission.
2. Set `ACCOUNT_SECURITY_MODE=ENCRYPTED_ONLY` on every application instance.
3. Run `2026-07-17_account_encryption_cutover.sql` with a MySQL script-capable client.
   The script aborts if any populated plaintext value lacks ciphertext.
4. Confirm `remaining_plaintext` is zero.

Production must run with a `prod` or `production` Spring profile. In those profiles
the application refuses to start in `LEGACY_READ` mode or without the active key.

## Controlled rollback

Before running the schema rollback after phase 2:

1. Keep all historical keys configured.
2. Run one instance with:

   ```text
   ACCOUNT_SECURITY_MODE=ENCRYPTED_ONLY
   ACCOUNT_DATA_BACKFILL_ENABLED=true
   ACCOUNT_DATA_BACKFILL_DIRECTION=DECRYPT
   ```

3. Wait for `withdrawal_account_plaintext_restore_complete`, verify the plaintext
   count, disable the backfill, and then run
   `2026-07-17_account_withdrawal_hardening_rollback.sql`.

Never log key material, ciphertext, plaintext payout details, or trade passwords.
