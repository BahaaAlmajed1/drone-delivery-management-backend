package com.example.dronedelivery.repo;

import com.example.dronedelivery.domain.Drone;
import com.example.dronedelivery.domain.DroneStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DroneRepository extends JpaRepository<Drone, UUID> {
    Optional<Drone> findByName(String name);

    List<Drone> findByStatusIn(Collection<DroneStatus> statuses);

    List<Drone> findByStatusInAndCurrentJobIdIsNull(Collection<DroneStatus> statuses);
}
