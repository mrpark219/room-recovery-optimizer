CREATE TABLE recovery_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    accommodation_id BIGINT NOT NULL,
    session_id VARCHAR(36) NOT NULL,
    reference_date DATE NOT NULL,
    action VARCHAR(24) NOT NULL,
    recorded_at TIMESTAMP(6) NOT NULL,
    details JSON NOT NULL,
    INDEX ix_recovery_history_accommodation (accommodation_id, id)
);
