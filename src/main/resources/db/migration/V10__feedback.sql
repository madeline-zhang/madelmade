-- Feedback and bug reports sent from the little button in the corner of every page.
create table feedback (
    id         bigserial primary key,
    kind       varchar(20)  not null default 'feedback',
    message    text         not null,
    email      varchar(200),
    page       varchar(500),
    resolved   boolean      not null default false,
    created_at timestamptz  not null default now()
);
