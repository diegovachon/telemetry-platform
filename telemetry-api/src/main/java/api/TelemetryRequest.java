package api;

/**
 * What the agent will send in a POST /telemetry request body:
 * { "cpuPercent": 12.3, "ramPercent": 61.7 }
 *
 * Kept as a separate class from TelemetrySnapshot on purpose — the
 * request shape and the DB schema are allowed to diverge later
 * (e.g. if we add fields to one without touching the other).
 */
public class TelemetryRequest {
    private double cpuPercent;
    private double ramPercent;

    public double getCpuPercent() {
        return cpuPercent;
    }

    public void setCpuPercent(double cpuPercent) {
        this.cpuPercent = cpuPercent;
    }

    public double getRamPercent() {
        return ramPercent;
    }

    public void setRamPercent(double ramPercent) {
        this.ramPercent = ramPercent;
    }
}
