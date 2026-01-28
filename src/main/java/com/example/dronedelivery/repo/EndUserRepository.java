package com.example.dronedelivery.repo;

import com.example.dronedelivery.domain.EndUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EndUserRepository extends JpaRepository<EndUser, UUID> {
    Optional<EndUser> findByName(String name);
}
