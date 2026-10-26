-- Where this order is picked up, when Madeline picks somewhere other than her home address.
alter table orders add column pickup_address varchar(500);
