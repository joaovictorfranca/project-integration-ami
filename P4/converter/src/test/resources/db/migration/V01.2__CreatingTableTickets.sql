CREATE TABLE tickets (
                         id UUID PRIMARY KEY,
                         status tickets_status NOT NULL DEFAULT 'OPEN',
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);