-- Give the previously unassigned demo reservation its own physical room.
INSERT INTO room (id, accommodation_id, room_type_id, name) VALUES (5, 1, 1, '103');
INSERT INTO room_assignment (id, accommodation_id, reservation_id, room_id, start_date, `endDate`)
VALUES (3, 1, 3, 5, '2026-11-10', '2026-11-12');
