-- Hot or iced, beans and milk are the same on every latte, so each becomes one shared group.
-- The first latte's copy is kept; the other lattes point at it and their copies are removed.
do $$
declare
    group_name text;
    keep_id    bigint;
begin
    foreach group_name in array array['hot or iced', 'beans (double shot)', 'milk'] loop
        select g.id into keep_id
        from option_group g join product p on p.id = g.product_id join category c on c.id = p.category_id
        where c.slug = 'coffee' and lower(g.name) = group_name
        order by p.display_order, p.id, g.id limit 1;
        if keep_id is null then continue; end if;

        insert into product_shared_group (product_id, group_id)
        select distinct g.product_id, keep_id
        from option_group g join product p on p.id = g.product_id join category c on c.id = p.category_id
        where c.slug = 'coffee' and lower(g.name) = group_name;

        delete from option_group g using product p, category c
        where p.id = g.product_id and c.id = p.category_id
          and c.slug = 'coffee' and lower(g.name) = group_name and g.id <> keep_id;

        update option_group set product_id = null where id = keep_id;
    end loop;
end $$;
