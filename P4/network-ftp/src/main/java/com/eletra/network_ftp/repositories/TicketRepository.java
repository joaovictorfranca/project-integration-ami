package com.eletra.network_ftp.repositories;

import com.eletra.network_ftp.model.entities.TicketsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<TicketsEntity, UUID> {
}
