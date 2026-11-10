-- 독립된 세션 데모 숙소. 종료일은 숙박에 포함하지 않는다.
INSERT INTO accommodation (id, name)
VALUES (3, '복구 세션 데모 호텔');
INSERT INTO room_type (id, accommodation_id, name, capacity, grade, nightly_rate)
VALUES (4, 3, 'Standard', 2, 1, 100000);
INSERT INTO room (id, accommodation_id, room_type_id, name)
VALUES (101, 3, 4, '101'),
       (102, 3, 4, '102'),
       (201, 3, 4, '201'),
       (202, 3, 4, '202');
INSERT INTO reservation (id, accommodation_id, booked_room_type_id, booked_at, guest_count, start_date, `endDate`,
                         status)
VALUES (1001, 3, 4, '2026-10-01 00:00:00', 2, '2026-11-10', '2026-11-15', 'CHECKED_IN'),
       (1002, 3, 4, '2026-10-02 00:00:00', 2, '2026-11-10', '2026-11-15', 'CHECKED_IN'),
       (1003, 3, 4, '2026-10-03 00:00:00', 2, '2026-11-13', '2026-11-15', 'CONFIRMED'),
       (1004, 3, 4, '2026-10-04 00:00:00', 2, '2026-11-11', '2026-11-13', 'CHECKED_IN');
INSERT INTO room_assignment (id, accommodation_id, reservation_id, room_id, start_date, `endDate`)
VALUES (1001, 3, 1001, 101, '2026-11-10', '2026-11-15'),
       (1002, 3, 1002, 102, '2026-11-10', '2026-11-15'),
       (1003, 3, 1003, 201, '2026-11-13', '2026-11-15'),
       (1004, 3, 1004, 202, '2026-11-11', '2026-11-13');
