package com.test1.test1.repositories;

import com.test1.test1.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByNormalizedPhoneNumber(String normalizedPhoneNumber);
}