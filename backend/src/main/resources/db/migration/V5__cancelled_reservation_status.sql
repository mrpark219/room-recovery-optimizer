ALTER TABLE reservation DROP CHECK ck_res_status;
ALTER TABLE reservation
    ADD CONSTRAINT ck_res_status CHECK (status IN ('CONFIRMED', 'CHECKED_IN', 'CANCELLED'));
