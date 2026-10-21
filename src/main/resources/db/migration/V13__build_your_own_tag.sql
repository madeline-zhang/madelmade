-- "Build your own" on menu cards is now a per-product switch instead of following from having options.
alter table product add column build_your_own_tag boolean not null default false;
update product set build_your_own_tag = true where slug in ('classic-loaf', 'stencil-loaf');
