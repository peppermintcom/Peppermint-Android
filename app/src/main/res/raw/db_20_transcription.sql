ALTER TABLE tbl_recording ADD COLUMN transcription text;
ALTER TABLE tbl_recording ADD COLUMN transcription_confidence decimal(10,5);
ALTER TABLE tbl_recording ADD COLUMN transcription_lang varchar(128);
ALTER TABLE tbl_recording ADD COLUMN transcription_url varchar(255);
UPDATE tbl_recording SET transcription = (SELECT transcription FROM tbl_message WHERE tbl_message.recording_id = tbl_recording.recording_id);