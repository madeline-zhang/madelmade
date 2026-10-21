-- Flavors and other choices can add their own ingredients and allergens to the product's base list.
alter table option_choice add column ingredients text;
alter table option_choice add column allergens varchar(300);
