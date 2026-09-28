package api;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

/**
 * One row per poll from the agent: a CPU/RAM reading at a point in time.
 * Hibernate creates the backing table automatically (see
 * spring.jpa.hibernate.ddl-auto in application.properties) — no manual
 * CREATE TABLE needed while iterating locally.
 */
@Entity
public class TelemetrySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime recordedAt;
    private double cpuPercent;
    private double ramPercent;

    // JPA requires a no-arg constructor
    protected TelemetrySnapshot() {
    }

    public TelemetrySnapshot(LocalDateTime recordedAt, double cpuPercent, double ramPercent) {
        this.recordedAt = recordedAt;
        this.cpuPercent = cpuPercent;
        this.ramPercent = ramPercent;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public double getCpuPercent() {
        return cpuPercent;
    }

    public double getRamPercent() {
        return ramPercent;
    }
}
