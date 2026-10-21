-- Sales tax: each product has a tax kind, and each order keeps the rates and amounts it was charged,
-- so changing a rate later never rewrites past orders.
alter table product add column tax_category varchar(20) not null default 'PREPARED';
alter table order_item add column tax_category varchar(20) not null default 'PREPARED';
alter table orders add column tax_enabled boolean not null default false;
alter table orders add column tax_included boolean not null default false;
alter table orders add column tax_rate_prepared numeric(6,3) not null default 0;
alter table orders add column tax_rate_food numeric(6,3) not null default 0;
alter table orders add column tax_rate_local numeric(6,3) not null default 0;
alter table orders add column taxable_prepared numeric(10,2) not null default 0;
alter table orders add column taxable_food numeric(10,2) not null default 0;
alter table orders add column non_taxable numeric(10,2) not null default 0;
alter table orders add column state_tax numeric(10,2) not null default 0;
alter table orders add column local_tax numeric(10,2) not null default 0;
alter table orders add column tax numeric(10,2) not null default 0;
