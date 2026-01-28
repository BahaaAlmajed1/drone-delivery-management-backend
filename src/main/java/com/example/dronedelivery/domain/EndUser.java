package com.example.dronedelivery.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "end_users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_end_users_name", columnNames = {"name"})
})
public class EndUser {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    protected EndUser() {}

    public EndUser(String name) {
        this.name = name;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
}
