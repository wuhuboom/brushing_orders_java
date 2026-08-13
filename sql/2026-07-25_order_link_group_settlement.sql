-- Align legacy unfinished link orders with the original site's sequence rule:
-- an order configured for sequence N is matched while task progress is N - 1.
UPDATE order_info oi
JOIN order_link ol ON ol.id = oi.link_id
SET oi.order_count = oi.order_count + 1
WHERE oi.type = '1'
  AND oi.status = '1'
  AND ol.status = '1';

UPDATE order_link
SET order_count = order_count + 1
WHERE status = '1';
