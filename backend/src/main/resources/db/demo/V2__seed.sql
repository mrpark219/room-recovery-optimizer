INSERT INTO accommodation (id,name) VALUES (1,'한강 호텔'),(2,'바다 호텔');
INSERT INTO room_type (id,accommodation_id,name,capacity,grade,nightly_rate) VALUES (1,1,'Standard',2,1,100000),(2,1,'Deluxe',4,2,150000),(3,2,'Standard',2,1,90000);
INSERT INTO room (id,accommodation_id,room_type_id,name) VALUES (1,1,1,'101'),(2,1,1,'102'),(3,1,2,'201'),(4,2,3,'101');
INSERT INTO reservation (id,accommodation_id,booked_room_type_id,occupied_room_id,booked_at,guest_count,start_date,end_date_exclusive,status) VALUES
 (1,1,1,1,'2026-10-01 00:00:00',2,'2026-11-09','2026-11-12','CHECKED_IN'),(2,1,1,NULL,'2026-10-02 00:00:00',2,'2026-11-10','2026-11-13','CONFIRMED'),(3,1,1,NULL,'2026-10-03 00:00:00',2,'2026-11-10','2026-11-12','CONFIRMED');
INSERT INTO room_assignment (id,accommodation_id,reservation_id,room_id,start_date,end_date_exclusive) VALUES
 (1,1,1,1,'2026-11-09','2026-11-12'),(2,1,2,2,'2026-11-10','2026-11-13');
