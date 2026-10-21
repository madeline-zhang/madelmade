-- The photo editor now has contrast and saturation, and every slider runs from -100 to 100 with 0 as no change.
alter table photo add column contrast int not null default 0;
alter table photo add column saturation int not null default 0;
update photo set warmth = (warmth - 5) * 20, brightness = (brightness - 5) * 20;
update setting set value = ((value::int - 5) * 20)::text
  where key in ('photos.warmth', 'photos.brightness') and value ~ '^[0-9]+$';
-- Crops are now a rectangle the editor draws; older crop names fall back to a centered crop.
