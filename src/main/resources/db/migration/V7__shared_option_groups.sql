-- Option groups that several products share, like Flavor on the Classic and Stencil loaves.
-- A shared group has no product of its own; product_shared_group says which products use it.
alter table option_group alter column product_id drop not null;

create table product_shared_group (
    product_id bigint not null references product (id) on delete cascade,
    group_id   bigint not null references option_group (id) on delete cascade,
    primary key (product_id, group_id)
);

-- Turn the Classic Loaf's Flavor group into a shared one and point the Stencil Loaf at it,
-- replacing the Stencil Loaf's own copy.
do $$
declare
    classic_id bigint;
    stencil_id bigint;
    flavor_id  bigint;
    old_id     bigint;
begin
    select id into classic_id from product where slug = 'classic-loaf';
    select id into stencil_id from product where slug = 'stencil-loaf';
    if classic_id is null then return; end if;
    select id into flavor_id from option_group where product_id = classic_id and lower(name) = 'flavor' order by id limit 1;
    if flavor_id is null then return; end if;

    update option_group set product_id = null where id = flavor_id;
    insert into product_shared_group (product_id, group_id) values (classic_id, flavor_id);

    if stencil_id is not null then
        select id into old_id from option_group where product_id = stencil_id and lower(name) = 'flavor' order by id limit 1;
        if old_id is not null then delete from option_group where id = old_id; end if;
        insert into product_shared_group (product_id, group_id) values (stencil_id, flavor_id);
    end if;
end $$;
