-- Home page hero: photos (a computer crop and an optional phone crop) or a looping video, shown in turn.
create table hero_video (
    id           bigserial primary key,
    data         bytea        not null,
    content_type varchar(60)  not null,
    updated_at   timestamptz  not null default now()
);

create table hero_slide (
    id             bigserial primary key,
    photo_id       bigint references photo (id) on delete set null,
    phone_photo_id bigint references photo (id) on delete set null,
    video_id       bigint references hero_video (id) on delete set null,
    alt            varchar(200),
    display_order  int          not null default 0
);
