import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Step 1: CPU/RAM polling (verified).
 * Step 2: process liveness detection (verified).
 * Step 3: log tailing / error detection (verified).
 * Step 4: (separate project) Spring Boot API skeleton (verified).
 * Step 5: agent now POSTs CPU/RAM to the API instead of just printing.
 *
 * Registration convention: any script you want monitored writes its own
 * PID to a file at ~/.telemetry/<name>.pid when it starts (one line,
 * just the PID as text). The agent has no way to discover "your"
 * scripts on its own, so this file is the registration mechanism.
 *
 * Logging convention (step 3): each registered script also writes plain
 * text log lines to ~/.telemetry/<name>.log, each line starting with a
 * severity tag: "INFO:", "WARN:", or "ERROR:". The agent tails only
 * WARN/ERROR lines written since its last poll.
 *
 * Console output is deliberately trimmed as of step 5: routine "all
 * good" states (RUNNING processes, successful sends) print one compact
 * line; only anomalies (CRASHED, failed sends, WARN/ERROR log lines)
 * get their own line, since the API is now the real destination for
 * this data — the console is just a sanity-check window into it.
 */
public class TelemetryAgent {

    private static final int POLL_INTERVAL_SECONDS = 5;
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Path TELEMETRY_DIR =
            Paths.get(System.getProperty("user.home"), ".telemetry");
    private static final String API_URL = "http://localhost:8080/telemetry";
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static String apiKey; // resolved once in main(), before the polling loop starts

    // Tracks how many bytes of each log file we've already read, so we
    // only process lines written since the last poll.
    private static final Map<String, Long> logOffsets = new HashMap<>();

    public static void main(String[] args) throws InterruptedException, IOException {
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        CentralProcessor processor = hardware.getProcessor();
        GlobalMemory memory = hardware.getMemory();

        Files.createDirectories(TELEMETRY_DIR);
        apiKey = resolveApiKey(); // fails fast here if TELEMETRY_API_KEY isn't set

        System.out.println("Telemetry agent started. Polling every "
                + POLL_INTERVAL_SECONDS + "s. Watching " + TELEMETRY_DIR
                + " for registered processes. Press Ctrl+C to stop.\n");

        // CPU load is a delta measurement: OSHI needs two snapshots of the
        // processor's "ticks" (time spent in each state: user, idle, etc.)
        // to compute a load percentage over the interval between them.
        // We take the first snapshot here, before the loop starts.
        long[] previousTicks = processor.getSystemCpuLoadTicks();

        while (true) {
            Thread.sleep(POLL_INTERVAL_SECONDS * 1000L);

            // Second snapshot — OSHI diffs this against previousTicks
            // internally to compute load over the interval that just elapsed.
            double cpuLoad = processor.getSystemCpuLoadBetweenTicks(previousTicks) * 100;
            previousTicks = processor.getSystemCpuLoadTicks();

            long totalMemory = memory.getTotal();
            long availableMemory = memory.getAvailable();
            long usedMemory = totalMemory - availableMemory;
            double ramLoad = (usedMemory / (double) totalMemory) * 100;

            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            boolean sent = sendTelemetry(cpuLoad, ramLoad);

            System.out.printf(
                    "[%s] CPU: %5.1f%%  RAM: %5.1f%%  -> API: %s%n",
                    timestamp, cpuLoad, ramLoad, sent ? "ok" : "FAILED (is the API running?)"
            );

            reportProcessStatus();
            reportNewLogErrors();
        }
    }

    /**
     * Reads the shared API key from the TELEMETRY_API_KEY environment
     * variable -- same variable the API reads, so both sides always
     * agree without a second config file to keep in sync. Called once
     * at startup; fails fast with a clear message if it's not set,
     * rather than crashing repeatedly mid-poll-loop.
     */
    private static String resolveApiKey() {
        String key = System.getenv("TELEMETRY_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "TELEMETRY_API_KEY environment variable is not set. "
                            + "Export it before running the agent, e.g.: export TELEMETRY_API_KEY=your-secret-here");
        }
        return key;
    }

    /**
     * POSTs one CPU/RAM reading to the API as JSON. Locale.US is forced
     * explicitly here — without it, String.format uses the system's
     * default locale, and on a French-Canadian locale that renders
     * decimals with a comma (e.g. "6,5"), which would produce invalid
     * JSON. Returns false (rather than throwing) on any failure, so one
     * bad send doesn't kill the polling loop.
     */
    private static boolean sendTelemetry(double cpuPercent, double ramPercent) {
        String json = String.format(
                Locale.US,
                "{\"cpuPercent\": %.2f, \"ramPercent\": %.2f}",
                cpuPercent, ramPercent
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("X-API-Key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 201;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    /**
     * Scans TELEMETRY_DIR for *.pid files. Each one represents a script
     * that registered itself; the filename (minus .pid) is its display
     * name, and the file's contents is the PID to check for liveness.
     *
     * Trimmed as of step 5: only prints something when a process is
     * CRASHED or its PID file is unreadable. A healthy RUNNING process
     * produces no console output — the API now holds that state, so
     * the console only needs to flag what needs your attention.
     */
    private static void reportProcessStatus() throws IOException {
        try (DirectoryStream<Path> pidFiles =
                     Files.newDirectoryStream(TELEMETRY_DIR, "*.pid")) {

            for (Path pidFile : pidFiles) {
                String fileName = pidFile.getFileName().toString();
                String processName = fileName.substring(0, fileName.length() - ".pid".length());

                try {
                    long pid = Long.parseLong(Files.readString(pidFile).trim());
                    boolean alive = ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
                    if (!alive) {
                        System.out.printf("             !! %-20s CRASHED (pid %d not found)%n", processName, pid);
                    }
                } catch (NumberFormatException e) {
                    System.out.printf("             !! %-20s INVALID (pid file unreadable)%n", processName);
                }
            }
        }
    }

    /**
     * Scans TELEMETRY_DIR for *.log files. For each one, reads only the
     * bytes written since the last poll (tracked in logOffsets), splits
     * that new content into lines, and prints any line tagged WARN/ERROR.
     */
    private static void reportNewLogErrors() throws IOException {
        try (DirectoryStream<Path> logFiles =
                     Files.newDirectoryStream(TELEMETRY_DIR, "*.log")) {

            for (Path logFile : logFiles) {
                String fileName = logFile.getFileName().toString();
                String processName = fileName.substring(0, fileName.length() - ".log".length());

                long currentLength = Files.size(logFile);
                long lastOffset = logOffsets.getOrDefault(fileName, 0L);

                // File got truncated or replaced (e.g. script restarted) —
                // reset to the start rather than crash on a negative read length.
                if (currentLength < lastOffset) {
                    lastOffset = 0L;
                }

                if (currentLength > lastOffset) {
                    try (RandomAccessFile raf = new RandomAccessFile(logFile.toFile(), "r")) {
                        raf.seek(lastOffset);
                        String line;
                        while ((line = raf.readLine()) != null) {
                            if (line.startsWith("WARN:") || line.startsWith("ERROR:")) {
                                System.out.printf("             !! [%s] %s%n", processName, line);
                            }
                        }
                        lastOffset = raf.getFilePointer();
                    }
                }

                logOffsets.put(fileName, lastOffset);
            }
        }
    }
}
