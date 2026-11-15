-- One-time migration for the chat / trade-status feature.
-- Run against gawt_db BEFORE starting the updated backend:
--   mysql -u root -p gawt_db < src/main/resources/db/chat_migration.sql
--
-- Item.status is now only AVAILABLE | TRADED (RESERVED and SOLD were removed).
-- Hibernate created `status` as a MySQL ENUM('AVAILABLE','RESERVED','SOLD'),
-- and ddl-auto=update will not change it, so convert it to VARCHAR first.

ALTER TABLE items MODIFY status VARCHAR(20) NULL;

UPDATE items SET status = 'AVAILABLE' WHERE status = 'RESERVED' OR status IS NULL;
UPDATE items SET status = 'TRADED'    WHERE status = 'SOLD';

-- The new tables (conversations, messages) and the new item columns
-- (traded_with_user_id, traded_at) are created automatically by Hibernate
-- on startup (spring.jpa.hibernate.ddl-auto=update).

-- ------------------------------------------------------------------
-- Trading only: price, negotiable flag and trade option were removed
-- from the code. Hibernate will not change these columns itself, and
-- trade_option / is_negotiable were NOT NULL, so new items failed to
-- insert. Make them optional (keeps existing data):
-- ------------------------------------------------------------------
ALTER TABLE items MODIFY trade_option VARCHAR(20) NULL;
ALTER TABLE items MODIFY is_negotiable BIT(1) NULL DEFAULT b'0';

-- OPTIONAL, later: permanently remove the old columns (deletes old prices).
-- ALTER TABLE items DROP COLUMN price;
-- ALTER TABLE items DROP COLUMN is_negotiable;
-- ALTER TABLE items DROP COLUMN trade_option;
