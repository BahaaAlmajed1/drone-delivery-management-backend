package com.example.dronedelivery.repo;

import com.example.dronedelivery.domain.Job;
import com.example.dronedelivery.domain.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {
    List<Job> findByStatusOrderByCreatedAtAsc(JobStatus status);
    Optional<Job> findByOrderIdAndStatusIn(UUID orderId, List<JobStatus> statuses);
}
