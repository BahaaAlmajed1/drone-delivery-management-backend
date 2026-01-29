package com.example.dronedelivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DroneDeliveryApplication {
    public static void main(String[] args) {
        SpringApplication.run(DroneDeliveryApplication.class, args);
    }
}
