# Telemetry Platform

A local development telemetry platform that collects CPU and memory metrics and displays them in an Angular dashboard.

## Components

- `telemetry-agent`: Java 17 agent that samples system metrics every five seconds.
- `telemetry-api`: Spring Boot API backed by a local H2 database.
- `telemetry-dashboard/telemetry-dashboard`: Angular dashboard that refreshes every ten seconds.

## Run locally

Use the same local-development API key for the API and agent:

```sh
export TELEMETRY_API_KEY=local-dev-only
```

Start each component in a separate terminal:

```sh
cd telemetry-api
mvn spring-boot:run
```

```sh
cd telemetry-agent
mvn exec:java
```

```sh
cd telemetry-dashboard/telemetry-dashboard
npm install
npm start
```

Open [http://localhost:4200](http://localhost:4200).

The API listens on port `8080`, and the dashboard listens on port `4200`.

## Security

The included API key is intentionally a non-secret local-development value. Browser-delivered API keys are visible to users and must not be treated as production authentication.
