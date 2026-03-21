package com.muratsag.kafkaboard.cluster;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClusterRepository extends JpaRepository<ClusterEntity, UUID> {
    List<ClusterEntity> findAllByUser_IdOrderByCreatedAtDesc(UUID userId);

    Optional<ClusterEntity> findByIdAndUser_Id(UUID id, UUID userId);
}
