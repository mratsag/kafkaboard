package com.muratsag.kafkaboard.cluster;

import com.muratsag.kafkaboard.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "clusters")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false)
    private String name;

    @Column(name = "bootstrap_servers", nullable = false, length = 500)
    private String bootstrapServers;

    @Column(name = "security_protocol", length = 20)
    @Builder.Default
    private String securityProtocol = "PLAINTEXT";

    @Column(name = "sasl_mechanism", length = 20)
    private String saslMechanism;

    @Column(name = "sasl_username", length = 255)
    private String saslUsername;

    @Column(name = "sasl_password_encrypted")
    private String saslPasswordEncrypted;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
