package com.example.dronedelivery.service;

import com.example.dronedelivery.domain.Drone;
import com.example.dronedelivery.domain.EndUser;
import com.example.dronedelivery.repo.DroneRepository;
import com.example.dronedelivery.repo.EndUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityService {

    private final DroneRepository droneRepository;
    private final EndUserRepository endUserRepository;

    public IdentityService(DroneRepository droneRepository, EndUserRepository endUserRepository) {
        this.droneRepository = droneRepository;
        this.endUserRepository = endUserRepository;
    }

    @Transactional
    public Drone getOrCreateDrone(String name) {
        return droneRepository.findByName(name).orElseGet(() -> droneRepository.save(new Drone(name)));
    }

    @Transactional
    public EndUser getOrCreateEndUser(String name) {
        return endUserRepository.findByName(name).orElseGet(() -> endUserRepository.save(new EndUser(name)));
    }
}
