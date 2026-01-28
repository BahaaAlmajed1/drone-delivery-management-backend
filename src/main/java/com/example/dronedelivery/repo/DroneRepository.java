package com.example.dronedelivery.repo;

import com.example.dronedelivery.domain.Drone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DroneRepository extends JpaRepository<Drone, UUID> {
    Optional<Drone> findByName(String name);
}
