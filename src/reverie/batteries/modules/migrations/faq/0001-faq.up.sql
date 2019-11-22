CREATE TABLE batteries_faq_entry (
       id serial primary key not null,
       question text not null default '',
       answer text not null default '',
       type text not null default '',
       ordering integer not null default 0,
       visible_p boolean not null default false
);
