-- Tables
CREATE TABLE IF NOT EXISTS tbl_pending_logout (pending_logout_id INTEGER PRIMARY KEY AUTOINCREMENT, account_server_id varchar(255), device_server_id varchar(255), auth_token text);

CREATE TABLE IF NOT EXISTS tbl_chat (chat_id INTEGER PRIMARY KEY AUTOINCREMENT, title varchar(255), last_message_ts varchar(19));

CREATE TABLE IF NOT EXISTS tbl_recipient (recipient_id INTEGER PRIMARY KEY AUTOINCREMENT, droid_contact_data_id bigint, droid_contact_raw_id bigint, droid_contact_id bigint, display_name varchar(255), via varchar(255), mimetype varchar(255), photo_uri text, added_ts varchar(19) NOT NULL, is_peppermint int default 0);

CREATE TABLE IF NOT EXISTS tbl_chat_recipient (recipient_id bigint NOT NULL, chat_id bigint NOT NULL, registration_ts varchar(19) NOT NULL, PRIMARY KEY (recipient_id, chat_id));

CREATE TABLE IF NOT EXISTS tbl_message (message_id INTEGER PRIMARY KEY AUTOINCREMENT, server_message_id varchar(255), server_canonical_url varchar(255), server_short_url varchar(255), email_subject varchar(255), email_body varchar(255), chat_id bigint, author_id bigint, recording_id bigint, sent int default 0, received int default 0, played int default 0, registration_ts varchar(19) NOT NULL);

CREATE TABLE IF NOT EXISTS tbl_message_recipient (message_id bigint NOT NULL, recipient_id bigint NOT NULL, sent int default 0, PRIMARY KEY (message_id, recipient_id));

CREATE TABLE IF NOT EXISTS tbl_recording (recording_id INTEGER PRIMARY KEY AUTOINCREMENT, file_path varchar(255), transcription text, transcription_confidence decimal(10,5), transcription_lang varchar(128), transcription_url varchar(255), duration_millis bigint, size_kb float, has_video int, recorded_ts varchar(19), content_type varchar(150));

-- Views
CREATE VIEW IF NOT EXISTS v_chat_peppermint AS SELECT tbl_chat.*, tmp.droid_contact_ids, (SELECT MAX(is_peppermint) FROM tbl_recipient INNER JOIN tbl_chat_recipient ON tbl_recipient.recipient_id = tbl_chat_recipient.recipient_id WHERE tbl_chat_recipient.chat_id = tbl_chat.chat_id GROUP BY tbl_chat_recipient.chat_id) AS is_peppermint FROM tbl_chat, (SELECT chat_id, GROUP_CONCAT(droid_contact_id, ',') as droid_contact_ids FROM (SELECT DISTINCT chat_id, droid_contact_id FROM tbl_chat_recipient, tbl_recipient WHERE tbl_recipient.recipient_id = tbl_chat_recipient.recipient_id ORDER BY chat_id ASC, droid_contact_id ASC) GROUP BY chat_id)tmp WHERE tbl_chat.chat_id = tmp.chat_id;

CREATE VIEW IF NOT EXISTS v_chat AS SELECT v_chat_peppermint.*, (CASE WHEN v_chat_peppermint.is_peppermint <= 0 THEN IFNULL((SELECT rel.chat_id FROM v_chat_peppermint AS rel WHERE rel.is_peppermint > 0 AND rel.droid_contact_ids = v_chat_peppermint.droid_contact_ids), 0) ELSE 0 END) AS peppermint_chat_id FROM v_chat_peppermint;

CREATE VIEW IF NOT EXISTS v_message AS SELECT tbl_message.*, tbl_message.chat_id AS merged_chat_id FROM tbl_message, v_chat WHERE v_chat.chat_id = tbl_message.chat_id AND v_chat.peppermint_chat_id = 0 UNION SELECT tbl_message.*, v_chat.peppermint_chat_id AS merged_chat_id FROM tbl_message, v_chat WHERE v_chat.chat_id = tbl_message.chat_id AND v_chat.peppermint_chat_id <> 0;