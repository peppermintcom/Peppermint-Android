ALTER TABLE tbl_chat ADD COLUMN display_name varchar(255);
ALTER TABLE tbl_chat ADD COLUMN via varchar(255);
ALTER TABLE tbl_chat ADD COLUMN mimetype varchar(255);

ALTER TABLE tbl_message ADD COLUMN display_name varchar(255);
ALTER TABLE tbl_message ADD COLUMN via varchar(255);
ALTER TABLE tbl_message ADD COLUMN mimetype varchar(255);

ALTER TABLE tbl_message DROP COLUMN recipient_id;