-- Keeps recordings that were not sent due to failure of internet connectivity
-- These will be later sent (retry) by the SenderService
CREATE TABLE tbl_sending_request (sending_request_uuid varchar(100), subject varchar(255), body varchar(255), recording_id bigint, recipient_id bigint, canonical_url varchar(255), short_url varchar(255), sent int default 0, registration_ts varchar(19), PRIMARY KEY(sending_request_uuid));

CREATE TABLE tbl_sending_request_recording (recording_id INTEGER PRIMARY KEY AUTOINCREMENT, file_path varchar(255), duration_millis bigint, size_kb float, has_video int, recorded_ts varchar(19), content_type varchar(150));

CREATE TABLE tbl_sending_request_recipient (recipient_id INTEGER PRIMARY KEY AUTOINCREMENT, contact_id bigint, mime_type varchar(255), photo_uri varchar(255), name varchar(255), via varchar(255), account_type varchar(255));
