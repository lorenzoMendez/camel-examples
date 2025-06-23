create table if not exists audit_log(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20)
);

CREATE INDEX idx_audit_event_type ON audit_log(event_type);
CREATE INDEX idx_audit_created_at ON audit_log(created_at);

insert into audit_log(event_type, description, status) values('LOGIN', 'Se hace un intento de login', 'SUCCESS');

select * from audit_log;