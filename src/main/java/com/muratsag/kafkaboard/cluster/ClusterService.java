package com.muratsag.kafkaboard.cluster;

import com.muratsag.kafkaboard.cluster.dto.ClusterDto;
import com.muratsag.kafkaboard.cluster.dto.CreateClusterRequest;
import com.muratsag.kafkaboard.exception.ForbiddenException;
import com.muratsag.kafkaboard.exception.ResourceNotFoundException;
import com.muratsag.kafkaboard.user.UserEntity;
import com.muratsag.kafkaboard.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClusterService {

    private final ClusterRepository clusterRepository;
    private final UserRepository userRepository;

    @Transactional
    public ClusterDto createCluster(UUID userId, CreateClusterRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Cluster adı boş olamaz");
        }
        if (request.getBootstrapServers() == null || request.getBootstrapServers().isBlank()) {
            throw new IllegalArgumentException("Bootstrap server adresi boş olamaz");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));

        ClusterEntity cluster = clusterRepository.save(
                ClusterEntity.builder()
                        .user(user)
                        .name(request.getName().trim())
                        .bootstrapServers(request.getBootstrapServers().trim())
                        .build()
        );

        return toDto(cluster);
    }

    @Transactional(readOnly = true)
    public List<ClusterDto> getClusters(UUID userId) {
        return clusterRepository.findAllByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClusterEntity getOwnedCluster(UUID clusterId, UUID userId) {
        ClusterEntity cluster = clusterRepository.findById(clusterId)
                .orElseThrow(() -> new ResourceNotFoundException("Cluster bulunamadı: " + clusterId));

        if (!cluster.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Bu cluster'a erişim yetkiniz yok");
        }

        return cluster;
    }

    @Transactional
    public void deleteCluster(UUID clusterId, UUID userId) {
        ClusterEntity cluster = getOwnedCluster(clusterId, userId);
        clusterRepository.delete(cluster);
    }

    private ClusterDto toDto(ClusterEntity cluster) {
        return ClusterDto.builder()
                .id(cluster.getId())
                .name(cluster.getName())
                .bootstrapServers(cluster.getBootstrapServers())
                .createdAt(cluster.getCreatedAt())
                .build();
    }
}
