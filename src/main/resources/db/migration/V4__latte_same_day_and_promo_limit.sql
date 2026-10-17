-- Lattes can be made the same day they're ordered.
update product set lead_time_days = 0
  where category_id in (select id from category where slug = 'coffee') and lead_time_days = 1;

-- Promo codes can be limited to one use per customer.
alter table promo_code add column once_per_customer boolean not null default false;
