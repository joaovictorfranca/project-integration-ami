-- Índice para busca rápida de todos os processos de um ticket específico
CREATE INDEX idx_process_ticket_id ON process(ticket_id);