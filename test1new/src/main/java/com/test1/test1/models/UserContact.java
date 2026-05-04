package com.test1.test1.models;

import com.test1.test1.enums.ContactStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "user_contacts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_owner_contact_hash",
                        columnNames = {"owner_user_id", "contact_phone_hash"}
                )
        },
        indexes = {
                @Index(name = "idx_user_contacts_owner", columnList = "owner_user_id"),
                @Index(name = "idx_user_contacts_hash", columnList = "contact_phone_hash")
        }
)
public class UserContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "contact_phone_hash", nullable = false, columnDefinition = "BINARY(32)")
    private byte[] contactPhoneHash;

    @Column(name = "contact_name", length = 255)
    private String contactName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContactStatus status = ContactStatus.ACTIVE;

//    @Column(name = "contact_changed_at", nullable = false)
//    private LocalDateTime contactChangedAt;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;
}