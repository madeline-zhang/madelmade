-- Example text in the "Anything special?" box on each product page.
alter table product add column notes_hint varchar(120);
update product set notes_hint = 'Higher hydration, if possible'
  where category_id in (select id from category where slug = 'bread');
update product set notes_hint = 'Less ice'
  where category_id in (select id from category where slug = 'coffee');
