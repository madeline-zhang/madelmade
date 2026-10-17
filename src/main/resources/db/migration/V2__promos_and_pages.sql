-- Promo codes Madeline hands to friends and family.
create table promo_code (
    id          bigserial primary key,
    code        varchar(40)  not null unique,
    percent_off int          not null check (percent_off between 1 and 100),
    active      boolean      not null default true,
    max_uses    int,
    times_used  int          not null default 0,
    expires_on  date,
    note        varchar(200),
    created_at  timestamptz  not null default now()
);

alter table orders add column promo_code varchar(40);
alter table orders add column promo_percent int;
alter table orders add column discount numeric(10, 2) not null default 0;

-- Gallery photos are either past loaves or "other bakes".
alter table gallery_photo add column section varchar(20) not null default 'loaves';

-- Items with milk (lattes) have to be handed over in person, so they can't be delivered.
alter table product add column pickup_only boolean not null default false;
alter table product add column ingredients text;
alter table product add column allergens varchar(300);
update product set pickup_only = true where slug in ('latte', 'cookie-butter-latte', 'banana-latte');

update product set badge = 'Yeast-risen' where slug = 'moms-best' and badge = 'Not sourdough';
