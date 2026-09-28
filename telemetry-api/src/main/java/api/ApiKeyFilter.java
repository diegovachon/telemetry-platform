package api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Step 6a: basic service-to-service auth via a shared secret.
 *
 * Every request must carry a header "X-API-Key" matching the value
 * configured in telemetry.api.key (sourced from the TELEMETRY_API_KEY
 * environment variable — see application.properties). Requests missing
 * it, or carrying the wrong value, get rejected with 401 before they
 * ever reach TelemetryController.
 *
 * This is a placeholder for real auth (Entra ID/OAuth), not the final
 * answer — appropriate for local dev and for proving the plumbing
 * works, not for anything internet-facing.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    @Value("${telemetry.api.key}")
    private String expectedApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        // Browser CORS preflight requests (OPTIONS) never carry custom
        // headers like X-API-Key -- that's the browser's doing, not
        // something a client can control. Let these through untouched;
        // CorsConfig handles whether the preflight itself is allowed.
        // The REAL request that follows still goes through the check
        // below as normal.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedKey = request.getHeader("X-API-Key");

        if (providedKey == null || !providedKey.equals(expectedApiKey)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid X-API-Key header");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
