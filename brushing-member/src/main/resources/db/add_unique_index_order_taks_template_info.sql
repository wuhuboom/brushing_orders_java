-- SQL migration: add unique constraint to prevent duplicate order_index per template
-- NOTE: Before applying this in production, run the duplicate report and resolve duplicates:
-- SELECT template_id, order_index, COUNT(*) c FROM order_taks_template_info GROUP BY template_id, order_index HAVING c > 1;

-- MySQL example:
ALTER TABLE order_taks_template_info
  ADD CONSTRAINT uk_template_orderindex UNIQUE KEY uk_template_orderindex (template_id, order_index);

-- If using another DB, adjust the syntax accordingly.
