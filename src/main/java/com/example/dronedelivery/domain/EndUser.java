package com.example.dronedelivery.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EndUser {

    @Id
    @GeneratedValue
    private UUID id;

    private String name;

    public EndUser(String name) {
        this.name = name;
    }
}
