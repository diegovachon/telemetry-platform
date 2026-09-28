#!/bin/zsh
# Fake overnight job — tests both step 2 (liveness) and step 3 (log tailing).
#
# Registration: writes its own PID so the agent can check liveness.
# Logging: writes INFO/WARN/ERROR-tagged lines to its own .log file,
# following the convention the agent expects.

mkdir -p ~/.telemetry
PID_FILE=~/.telemetry/dummy-overnight-job.pid
LOG_FILE=~/.telemetry/dummy-overnight-job.log

echo $$ > "$PID_FILE"
: > "$LOG_FILE"   # start with a clean log file each run

echo "Fake job started (pid $$)."
echo "  Registered PID at: $PID_FILE"
echo "  Writing logs to:   $LOG_FILE"
echo "Ctrl+C to kill it early and test the CRASHED case."

i=0
while true; do
    i=$((i + 1))
    sleep 6

    if (( i % 4 == 0 )); then
        echo "ERROR: simulated failure on iteration $i" >> "$LOG_FILE"
    elif (( i % 3 == 0 )); then
        echo "WARN: retry attempt $i took longer than expected" >> "$LOG_FILE"
    else
        echo "INFO: iteration $i completed normally" >> "$LOG_FILE"
    fi
done
