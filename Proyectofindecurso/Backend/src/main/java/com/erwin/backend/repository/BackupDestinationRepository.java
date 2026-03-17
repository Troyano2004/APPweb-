package com.erwin.backend.repository;

import com.erwin.backend.entities.BackupDestination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BackupDestinationRepository extends JpaRepository<BackupDestination, Long> {
}