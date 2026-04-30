CREATE TABLE process (
                         id UUID PRIMARY KEY,
                         ticket_id UUID NOT NULL, -- FK que permite múltiplos processos por ticket
                         type process_type NOT NULL,
                         status process_status NOT NULL DEFAULT 'PENDING',
                         payload TEXT, -- Aqui você guarda a mensagem específica de cada etapa
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT fk_tickets_process
                             FOREIGN KEY (ticket_id)
                                 REFERENCES tickets (id)
                                 ON DELETE CASCADE
);