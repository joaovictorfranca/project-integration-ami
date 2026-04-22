package com.eletra.business.repositories;

import com.eletra.business.model.entities.ProcessEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcessRepository extends JpaRepository<ProcessEntity, UUID> {
}