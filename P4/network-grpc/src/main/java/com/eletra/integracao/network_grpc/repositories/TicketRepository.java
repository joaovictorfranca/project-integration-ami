package com.eletra.integracao.network_grpc.repositories;

import com.eletra.integracao.network_grpc.models.entities.TicketsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<TicketsEntity, UUID> {
}
