-- MadelMade schema. Products and options are data, so new items are a form, not a code change.

create table setting (
    key   varchar(100) primary key,
    value text
);

create table photo (
    id            bigserial primary key,
    original      bytea,
    original_type varchar(60),
    edited        bytea       not null,
    edited_type   varchar(60) not null default 'image/jpeg',
    rotate        int         not null default 0,
    tilt          double precision not null default 0,
    crop          varchar(20) not null default 'original',
    warmth        int         not null default 0,
    brightness    int         not null default 0,
    width         int,
    height        int,
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now()
);

create table category (
    id            bigserial primary key,
    name          varchar(80) not null,
    slug          varchar(80) not null unique,
    display_order int         not null default 0
);

create table product (
    id                       bigserial primary key,
    category_id              bigint       not null references category (id),
    name                     varchar(120) not null,
    slug                     varchar(120) not null unique,
    short_description        varchar(200),
    description              text,
    base_price               numeric(8, 2) not null default 0,
    lead_time_days           int,
    weekly_limit             int,
    active                   boolean      not null default true,
    sold_out                 boolean      not null default false,
    display_order            int          not null default 0,
    counts_as_loaf           boolean      not null default true,
    allows_birth_certificate boolean      not null default false,
    badge                    varchar(60),
    photo_id                 bigint references photo (id) on delete set null,
    created_at               timestamptz  not null default now(),
    updated_at               timestamptz  not null default now()
);

create table option_group (
    id            bigserial primary key,
    product_id    bigint      not null references product (id) on delete cascade,
    name          varchar(80) not null,
    required      boolean     not null default true,
    multiple      boolean     not null default false,
    display_style varchar(20) not null default 'buttons',
    display_order int         not null default 0
);

create table option_choice (
    id                    bigserial primary key,
    group_id              bigint       not null references option_group (id) on delete cascade,
    name                  varchar(80)  not null,
    price_change          numeric(8, 2) not null default 0,
    halves_price          boolean      not null default false,
    price_tbd             boolean      not null default false,
    needs_design_approval boolean      not null default false,
    description           text,
    photo_id              bigint references photo (id) on delete set null,
    text_limit            int,
    is_default            boolean      not null default false,
    active                boolean      not null default true,
    display_order         int          not null default 0
);

create table customer (
    id           bigserial primary key,
    name         varchar(120),
    email        varchar(200) not null unique,
    phone        varchar(40),
    contact_pref varchar(20)  not null default 'EMAIL',
    address      text,
    google_id    varchar(100),
    created_at   timestamptz  not null default now(),
    last_sign_in timestamptz
);

create sequence orders_seq start with 1001 increment by 1;

create table orders (
    id               bigint primary key default nextval('orders_seq'),
    customer_id      bigint       not null references customer (id),
    status           varchar(30)  not null,
    fulfillment      varchar(20)  not null,
    contact_name     varchar(120) not null,
    contact_email    varchar(200) not null,
    contact_phone    varchar(40),
    contact_pref     varchar(20)  not null default 'EMAIL',
    delivery_address text,
    requested_date   date         not null,
    requested_time   varchar(40),
    priority         boolean      not null default false,
    priority_fee     numeric(8, 2) not null default 0,
    delivery_fee     numeric(8, 2),
    deposit_waived   boolean      not null default false,
    deposit_paid     boolean      not null default false,
    subtotal         numeric(8, 2) not null default 0,
    adjustment       numeric(8, 2) not null default 0,
    adjustment_note  varchar(200),
    total            numeric(8, 2) not null default 0,
    paid             boolean      not null default false,
    price_tbd        boolean      not null default false,
    over_capacity    boolean      not null default false,
    signed_in        boolean      not null default false,
    customer_note    text,
    private_notes    text,
    status_token     varchar(64)  not null unique,
    reminder_sent    boolean      not null default false,
    created_at       timestamptz  not null default now(),
    updated_at       timestamptz  not null default now(),
    approved_at      timestamptz
);

create index orders_status_idx on orders (status);
create index orders_requested_date_idx on orders (requested_date);
create index orders_customer_idx on orders (customer_id);

create table order_item (
    id              bigserial primary key,
    order_id        bigint       not null references orders (id) on delete cascade,
    product_id      bigint references product (id) on delete set null,
    product_name    varchar(120) not null,
    quantity        int          not null default 1,
    choice_ids      varchar(400),
    options_summary text,
    custom_notes    text,
    letters_text    varchar(60),
    unit_price      numeric(8, 2) not null,
    line_total      numeric(8, 2) not null,
    price_tbd       boolean      not null default false,
    counts_as_loaf  boolean      not null default true,
    has_certificate boolean      not null default false,
    cert_name       varchar(80),
    cert_message    text,
    cert_price      numeric(8, 2) not null default 0,
    cert_baked_at   varchar(60),
    cert_weight     varchar(40),
    cert_labor      varchar(40),
    cert_starter    varchar(60),
    display_order   int          not null default 0
);

create table order_message (
    id         bigserial primary key,
    order_id   bigint      not null references orders (id) on delete cascade,
    author     varchar(20) not null,
    body       text        not null,
    created_at timestamptz not null default now()
);

create table order_event (
    id         bigserial primary key,
    order_id   bigint      not null references orders (id) on delete cascade,
    old_status varchar(30),
    new_status varchar(30) not null,
    note       varchar(300),
    created_at timestamptz not null default now()
);

create table login_code (
    id         bigserial primary key,
    email      varchar(200) not null,
    code_hash  varchar(100) not null,
    expires_at timestamptz  not null,
    attempts   int          not null default 0,
    used       boolean      not null default false,
    created_at timestamptz  not null default now()
);

create index login_code_email_idx on login_code (email);

create table gallery_photo (
    id            bigserial primary key,
    photo_id      bigint       not null references photo (id) on delete cascade,
    caption       varchar(200),
    display_order int          not null default 0
);
