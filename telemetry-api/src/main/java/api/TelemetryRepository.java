package api;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA generates the implementation of this at runtime.
 * save(), findAll(), findById(), etc. all come for free — no SQL,
 * no boilerplate DAO code.
 */
public interface TelemetryRepository extends JpaRepository<TelemetrySnapshot, Long> {
}
