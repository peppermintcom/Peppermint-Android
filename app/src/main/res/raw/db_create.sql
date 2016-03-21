-- Keeps recordings that were not sent due to failure of internet connectivity
-- These will be later sent (retry) by the SenderService
CREATE TABLE tbl_chat (chat_id INTEGER PRIMARY KEY AUTOINCREMENT, title varchar(255), last_message_ts varchar(19));

CREATE TABLE tbl_chat_recipient (chat_recipient_id INTEGER PRIMARY KEY AUTOINCREMENT, chat_id bigint, raw_contact_id bigint, contact_id bigint, display_name varchar(255), via varchar(255), mimetype varchar(255), photo_uri text, added_ts varchar(19));

CREATE TABLE tbl_message (message_id INTEGER PRIMARY KEY AUTOINCREMENT, server_message_id varchar(255), transcription text, server_canonical_url varchar(255), server_short_url varchar(255), email_subject varchar(255), email_body varchar(255), chat_id bigint, author_id bigint, recording_id bigint, sent int default 0, received int default 0, played int default 0, registration_ts varchar(19), sent_inapp int default 0);

CREATE TABLE tbl_recording (recording_id INTEGER PRIMARY KEY AUTOINCREMENT, file_path varchar(255), duration_millis bigint, size_kb float, has_video int, recorded_ts varchar(19), content_type varchar(150));