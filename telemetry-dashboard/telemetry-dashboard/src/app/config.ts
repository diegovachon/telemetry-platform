// Step 7: hardcoded for now, same honest trade-off as the agent's
// TELEMETRY_API_KEY -- anyone opening browser dev tools can see this
// value once the app is running. Fine for local dev, NOT fine for
// anything deployed/internet-facing. Real auth (Entra ID login) is a
// later step once Azure is actually in the picture.
export const API_BASE_URL = 'http://localhost:8080';
export const API_KEY = 'local-dev-only'; // same value as TELEMETRY_API_KEY
