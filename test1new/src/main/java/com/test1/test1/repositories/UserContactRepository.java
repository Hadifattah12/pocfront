package com.test1.test1.repositories;

import com.test1.test1.models.UserContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserContactRepository extends JpaRepository<UserContact, Long> {

    Optional<UserContact> findByOwnerUserIdAndContactPhoneHash(
            Long ownerUserId,
            byte[] contactPhoneHash
    );
}