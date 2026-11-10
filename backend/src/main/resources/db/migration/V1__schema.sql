CREATE TABLE accommodation
(
    id   BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL
);
CREATE TABLE room_type
(
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    accommodation_id BIGINT         NOT NULL,
    name             VARCHAR(200)   NOT NULL,
    capacity         INT            NOT NULL,
    grade            INT            NOT NULL,
    nightly_rate     DECIMAL(12, 2) NOT NULL,
    CONSTRAINT fk_type_accommodation FOREIGN KEY (accommodation_id) REFERENCES accommodation (id),
    CONSTRAINT uq_type_scope UNIQUE (id, accommodation_id),
    CONSTRAINT ck_type_capacity CHECK (capacity > 0),
    CONSTRAINT ck_type_grade CHECK (grade >= 0),
    CONSTRAINT ck_type_rate CHECK (nightly_rate >= 0)
);
CREATE TABLE room
(
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    accommodation_id BIGINT       NOT NULL,
    room_type_id     BIGINT       NOT NULL,
    name             VARCHAR(200) NOT NULL,
    CONSTRAINT fk_room_type_scope FOREIGN KEY (room_type_id, accommodation_id) REFERENCES room_type (id, accommodation_id),
    CONSTRAINT uq_room_scope UNIQUE (id, accommodation_id),
    CONSTRAINT uq_room_name UNIQUE (accommodation_id, name)
);
CREATE TABLE reservation
(
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    accommodation_id    BIGINT       NOT NULL,
    booked_room_type_id BIGINT       NOT NULL,
    occupied_room_id    BIGINT NULL,
    booked_at           TIMESTAMP(6) NOT NULL,
    guest_count         INT          NOT NULL,
    start_date          DATE         NOT NULL,
    end_date_exclusive  DATE         NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    CONSTRAINT fk_res_type_scope FOREIGN KEY (booked_room_type_id, accommodation_id) REFERENCES room_type (id, accommodation_id),
    CONSTRAINT fk_res_occupied_room_scope FOREIGN KEY (occupied_room_id, accommodation_id) REFERENCES room (id, accommodation_id),
    CONSTRAINT ck_res_occupied_room CHECK ((status = 'CHECKED_IN' AND occupied_room_id IS NOT NULL) OR
                                           (status = 'CONFIRMED' AND occupied_room_id IS NULL)),
    CONSTRAINT uq_res_scope UNIQUE (id, accommodation_id),
    CONSTRAINT ck_res_guests CHECK (guest_count > 0),
    CONSTRAINT ck_res_dates CHECK (start_date < end_date_exclusive),
    CONSTRAINT ck_res_status CHECK (status IN ('CONFIRMED', 'CHECKED_IN'))
);
CREATE TABLE room_assignment
(
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
    accommodation_id   BIGINT NOT NULL,
    reservation_id     BIGINT NOT NULL,
    room_id            BIGINT NOT NULL,
    start_date         DATE   NOT NULL,
    end_date_exclusive DATE   NOT NULL,
    CONSTRAINT fk_assignment_res FOREIGN KEY (reservation_id, accommodation_id) REFERENCES reservation (id, accommodation_id),
    CONSTRAINT fk_assignment_room FOREIGN KEY (room_id, accommodation_id) REFERENCES room (id, accommodation_id),
    CONSTRAINT ck_assignment_dates CHECK (start_date < end_date_exclusive)
);
CREATE TABLE room_unavailability
(
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id            BIGINT       NOT NULL,
    start_date         DATE         NOT NULL,
    end_date_exclusive DATE         NOT NULL,
    reason             VARCHAR(500) NOT NULL,
    CONSTRAINT fk_unavailability_room FOREIGN KEY (room_id) REFERENCES room (id),
    CONSTRAINT ck_unavailability_dates CHECK (start_date < end_date_exclusive),
    CONSTRAINT ck_unavailability_reason CHECK (CHAR_LENGTH(TRIM(reason)) > 0)
);
