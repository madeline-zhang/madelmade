-- "Letters" design choice is now "Text", with a font the customer picks.
alter table order_item add column letters_font varchar(60);
update option_choice set name = 'Text' where name = 'Letters' and text_limit is not null;
