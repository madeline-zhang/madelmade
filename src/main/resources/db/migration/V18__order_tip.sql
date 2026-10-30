-- Tips customers give, recorded by Madeline. Kept apart from the total so they're never taxed.
alter table orders add column tip numeric(8, 2) not null default 0;
