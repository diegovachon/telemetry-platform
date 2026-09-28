package api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/telemetry")
public class TelemetryController {

    private final TelemetryRepository repository;

    // Spring injects the repository automatically — no "new" needed.
    public TelemetryController(TelemetryRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TelemetrySnapshot receiveTelemetry(@RequestBody TelemetryRequest request) {
        TelemetrySnapshot snapshot = new TelemetrySnapshot(
                LocalDateTime.now(),
                request.getCpuPercent(),
                request.getRamPercent()
        );
        return repository.save(snapshot);
    }

    // Not part of the agent's job — this is just so you can verify
    // saved rows via curl/browser without needing a DB client yet.
    @GetMapping
    public List<TelemetrySnapshot> listAll() {
        return repository.findAll();
    }
}
