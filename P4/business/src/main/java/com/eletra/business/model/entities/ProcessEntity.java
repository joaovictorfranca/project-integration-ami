package com.eletra.business.model.entities;

import com.eletra.business.model.entities.TicketsEntity;
import com.eletra.business.model.enums.ProcessStatus;
import com.eletra.business.model.enums.ProcessType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "process")
@Data
@NoArgsConstructor
public class ProcessEntity {

    @Id
    @GeneratedValue
    private UUID id;

    // Muitos processos para um Ticket
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private TicketsEntity ticket;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "type", columnDefinition = "process_type")
    private ProcessType type;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "process_status")
    private ProcessStatus status = ProcessStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Construtores, Getters e Setters
    public ProcessEntity(ProcessStatus statusUp, String payloadUp, ProcessType typeProcess, TicketsEntity ticket) {
        this.status = statusUp;
        this.payload = payloadUp;
        this.type = typeProcess;
        this.ticket = ticket;
    }
}