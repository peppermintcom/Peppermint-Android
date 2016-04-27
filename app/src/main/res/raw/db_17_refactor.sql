-- Stop using unique recipients for each Chat to avoid duplicate entries and make search easier
CREATE TABLE IF NOT EXISTS tbl_recipient (recipient_id INTEGER PRIMARY KEY AUTOINCREMENT, droid_contact_data_id bigint, droid_contact_raw_id bigint, droid_contact_id bigint, display_name varchar(255), via varchar(255), mimetype varchar(255), photo_uri text, added_ts varchar(19) NOT NULL, is_peppermint int default 0);
CREATE TABLE IF NOT EXISTS tbl_chat_recipient (chat_recipient_id INTEGER PRIMARY KEY AUTOINCREMENT, chat_id bigint, raw_contact_id bigint, contact_id bigint, display_name varchar(255), via varchar(255), mimetype varchar(255), photo_uri text, added_ts varchar(19));
INSERT INTO tbl_recipient(droid_contact_data_id, droid_contact_raw_id, droid_contact_id, display_name, via, mimetype, photo_uri, added_ts, is_peppermint) SELECT contact_id AS droid_contact_data_id, raw_contact_id AS droid_contact_raw_id, 0 AS droid_contact_id, display_name, via, mimetype, photo_uri, IFNULL(MIN(added_ts), '2016-01-01 00:00:00') AS added_ts, 0 AS is_peppermint FROM tbl_chat_recipient GROUP BY raw_contact_id, contact_id;

-- The tbl_recipient.droid_contact_id and tbl_recipient.is_peppermint fields still have to be filled by the DatabaseHelper
CREATE TEMPORARY TABLE tmp_chat_recipient (recipient_id bigint NOT NULL, chat_id bigint NOT NULL, registration_ts varchar(19) NOT NULL, PRIMARY KEY (recipient_id, chat_id));
INSERT INTO tmp_chat_recipient SELECT tbl_recipient.recipient_id, tbl_chat_recipient.chat_id, tbl_chat_recipient.added_ts AS registration_ts FROM tbl_chat_recipient, tbl_recipient WHERE tbl_chat_recipient.raw_contact_id = tbl_recipient.droid_contact_raw_id AND tbl_chat_recipient.contact_id = tbl_recipient.droid_contact_data_id;
DROP TABLE IF EXISTS tbl_chat_recipient;
CREATE TABLE IF NOT EXISTS tbl_chat_recipient (recipient_id bigint NOT NULL, chat_id bigint NOT NULL, registration_ts varchar(19) NOT NULL, PRIMARY KEY (recipient_id, chat_id));
INSERT INTO tbl_chat_recipient SELECT * FROM tmp_chat_recipient;
DROP TABLE IF EXISTS tmp_chat_recipient;

-- Associate recipients with messages as well to keep track of the recipients to whom the message has been sent
CREATE TABLE IF NOT EXISTS tbl_message_recipient (message_id bigint NOT NULL, recipient_id bigint NOT NULL, sent int default 0, PRIMARY KEY (message_id, recipient_id));
INSERT INTO tbl_message_recipient SELECT tbl_message.message_id, tbl_chat_recipient.recipient_id, tbl_message.sent FROM tbl_message, tbl_chat, tbl_chat_recipient WHERE tbl_message.chat_id = tbl_chat.chat_id AND tbl_chat.chat_id = tbl_chat_recipient.chat_id AND tbl_message.received = 0;

-- Views (same as in db_create.sql)
CREATE VIEW IF NOT EXISTS v_chat_peppermint AS SELECT tbl_chat.*, tmp.droid_contact_ids, (SELECT MAX(is_peppermint) FROM tbl_recipient INNER JOIN tbl_chat_recipient ON tbl_recipient.recipient_id = tbl_chat_recipient.recipient_id WHERE tbl_chat_recipient.chat_id = tbl_chat.chat_id GROUP BY tbl_chat_recipient.chat_id) AS is_peppermint FROM tbl_chat, (SELECT chat_id, GROUP_CONCAT(droid_contact_id, ',') as droid_contact_ids FROM (SELECT DISTINCT chat_id, droid_contact_id FROM tbl_chat_recipient, tbl_recipient WHERE tbl_recipient.recipient_id = tbl_chat_recipient.recipient_id ORDER BY chat_id ASC, droid_contact_id ASC) GROUP BY chat_id)tmp WHERE tbl_chat.chat_id = tmp.chat_id;

CREATE VIEW IF NOT EXISTS v_chat AS SELECT v_chat_peppermint.*, (CASE WHEN v_chat_peppermint.is_peppermint <= 0 THEN IFNULL((SELECT rel.chat_id FROM v_chat_peppermint AS rel WHERE rel.is_peppermint > 0 AND rel.droid_contact_ids = v_chat_peppermint.droid_contact_ids), 0) ELSE 0 END) AS peppermint_chat_id FROM v_chat_peppermint;

CREATE VIEW IF NOT EXISTS v_message AS SELECT tbl_message.*, tbl_message.chat_id AS merged_chat_id FROM tbl_message, v_chat WHERE v_chat.chat_id = tbl_message.chat_id AND v_chat.peppermint_chat_id = 0 UNION SELECT tbl_message.*, v_chat.peppermint_chat_id AS merged_chat_id FROM tbl_message, v_chat WHERE v_chat.chat_id = tbl_message.chat_id AND v_chat.peppermint_chat_id <> 0;