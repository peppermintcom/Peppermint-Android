-- 0 both audio and transcription
-- 1 only audio
-- 2 only transcription
ALTER TABLE tbl_chat ADD COLUMN send_mode int NOT NULL default 0;
ALTER TABLE tbl_recipient ADD COLUMN via_share varchar(255);