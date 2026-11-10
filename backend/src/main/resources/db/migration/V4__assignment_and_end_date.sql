ALTER TABLE reservation DROP CHECK ck_res_occupied_room;
ALTER TABLE reservation DROP FOREIGN KEY fk_res_occupied_room_scope;
ALTER TABLE reservation DROP COLUMN occupied_room_id;
ALTER TABLE reservation DROP CHECK ck_res_dates;
ALTER TABLE reservation RENAME COLUMN end_date_exclusive TO `endDate`;
ALTER TABLE reservation
    ADD CONSTRAINT ck_res_dates CHECK (start_date < `endDate`);
ALTER TABLE room_assignment DROP CHECK ck_assignment_dates;
ALTER TABLE room_assignment RENAME COLUMN end_date_exclusive TO `endDate`;
ALTER TABLE room_assignment
    ADD CONSTRAINT ck_assignment_dates CHECK (start_date < `endDate`);
ALTER TABLE room_unavailability DROP CHECK ck_unavailability_dates;
ALTER TABLE room_unavailability RENAME COLUMN end_date_exclusive TO `endDate`;
ALTER TABLE room_unavailability
    ADD CONSTRAINT ck_unavailability_dates CHECK (start_date < `endDate`);
