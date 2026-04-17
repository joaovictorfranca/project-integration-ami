-- 1. Tipos Enum
CREATE TYPE tickets_status AS ENUM ('OPEN', 'IN_PROCESS', 'DONE', 'FAILED');
CREATE TYPE process_status AS ENUM ('PENDING', 'PROCESSING', 'SUCCESS', 'ERROR');
CREATE TYPE process_type AS ENUM ('NETWORK_GRPC', 'BUSINESS', 'CONVERTER', 'NETWORK_FTP');

-- 2. Tabela de Tickets (O "Pai" da jornada)
CREATE TABLE tickets (
                         id UUID PRIMARY KEY,
                         status tickets_status NOT NULL DEFAULT 'OPEN',
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Tabela de Processos (As "Etapas" da jornada)
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

-- Índice para busca rápida de todos os processos de um ticket específico
CREATE INDEX idx_process_ticket_id ON process(ticket_id);