-- Quotes from customers, shown below the menu on the home page.
create table testimonial (
    id            bigserial primary key,
    quote         text         not null,
    name          varchar(80),
    detail        varchar(120),
    display_order int          not null default 0
);
