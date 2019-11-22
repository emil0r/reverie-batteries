CREATE TABLE batteries_faq_object (
       id serial primary key not null,
       object_id bigserial not null references reverie_object(id),
       type text not null default ''
);
