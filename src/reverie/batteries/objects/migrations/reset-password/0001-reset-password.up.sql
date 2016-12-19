CREATE TABLE batteries_reset_password (
       id BIGSERIAL PRIMARY KEY,
       object_id BIGINT NOT NULL REFERENCES reverie_object(id),

       -- titles
       title TEXT NOT NULL DEFAULT '',
       title_email_sent TEXT NOT NULL DEFAULT '',
       title_password_reset TEXT NOT NULL DEFAULT '',
       title_password TEXT NOT NULL DEFAULT '',
       title_expired_token TEXT NOT NULL DEFAULT '',
       title_no_user TEXT NOT NULL DEFAULT '',

       -- descriptions
       description TEXT NOT NULL DEFAULT '',
       description_email_sent TEXT NOT NULL DEFAULT '',
       description_password_reset TEXT NOT NULL DEFAULT '',
       description_password TEXT NOT NULL DEFAULT '',
       description_expired_token TEXT NOT NULL DEFAULT '',
       description_no_user TEXT NOT NULL DEFAULT '',

       -- email stuff
       email_subject TEXT NOT NULL DEFAULT '',
       email_body TEXT NOT NULL DEFAULT '',

       -- url to use for sending out email
       reset_url TEXT NOT NULL DEFAULT '',
       -- redirect to this url when successfully reset password
       redirect_url TEXT NOT NULL DEFAULT '',
       -- login after successful reset?
       login_p BOOLEAN NOT NULL DEFAULT FALSE
);
