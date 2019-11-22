CREATE TABLE batteries_faq_list (
       id serial primary key,
       type text not null,
       name text not null,
       active_p boolean not null default true
);
