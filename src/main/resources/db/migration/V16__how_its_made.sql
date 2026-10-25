-- Videos are now shared by the home page hero and the How it's made steps.
alter table hero_video rename to video;
alter sequence hero_video_id_seq rename to video_id_seq;

-- How it's made: a long scroll of steps, each with a photo or a looping video and a few lines of text.
create table process_step (
    id            bigserial primary key,
    title         varchar(120) not null,
    body          text,
    photo_id      bigint references photo (id) on delete set null,
    video_id      bigint references video (id) on delete set null,
    display_order int          not null default 0
);
