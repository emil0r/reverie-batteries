CREATE TABLE batteries_reset_password (
       id BIGSERIAL PRIMARY KEY,
       object_id BIGINT NOT NULL REFERENCES reverie_object(id),
       mode TEXT NOT NULL DEFAULT '',
       title TEXT NOT NULL DEFAULT '',
       description TEXT NOT NULL DEFAULT '',
       description_no_user TEXT NOT NULL DEFAULT '',
       description_email_sent TEXT NOT NULL DEFAULT '',
       description_handle_reset TEXT NOT NULL DEFAULT '',
       email_subject TEXT NOT NULL DEFAULT '',
       email_body TEXT NOT NULL DEFAULT '',
       forgot_url TEXT NOT NULL DEFAULT '',
       reset_url TEXT NOT NULL DEFAULT '',
       redirect_url TEXT NOT NULL DEFAULT ''
);
