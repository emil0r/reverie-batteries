CREATE TABLE batteries_faq_raw_question (
       id serial primary key,
       created timestamp without time zone not null default now(),
       name text not null default '',
       email text not null default '',
       phone text not null default '',
       skype text not null default '',
       question text not null default ''
);
