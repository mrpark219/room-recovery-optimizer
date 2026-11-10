-- Reject fractional or out-of-range legacy values before changing the column type.
ALTER TABLE room_type
    ADD CONSTRAINT ck_room_rate_integer_conversion
        CHECK (nightly_rate = FLOOR(nightly_rate) AND nightly_rate <= 2147483647);
ALTER TABLE room_type MODIFY COLUMN nightly_rate INT NOT NULL;
ALTER TABLE room_type DROP CHECK ck_room_rate_integer_conversion;
