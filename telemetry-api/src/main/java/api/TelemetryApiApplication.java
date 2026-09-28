package api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Step 4: the backend API skeleton.
 *
 * Receives what the agent used to only print to console, and persists
 * it to a real database instead. Everything here is deliberately
 * minimal: one entity, one endpoint, no auth yet (that's step 5),
 * no process/log tables yet (metrics only, for now).
 */
@SpringBootApplication
public class TelemetryApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(TelemetryApiApplication.class, args);
    }
}
