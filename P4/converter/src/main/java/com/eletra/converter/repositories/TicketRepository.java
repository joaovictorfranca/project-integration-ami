package com.eletra.converter.repositories;

import com.eletra.converter.model.entities.TicketsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<TicketsEntity, UUID> {
}
