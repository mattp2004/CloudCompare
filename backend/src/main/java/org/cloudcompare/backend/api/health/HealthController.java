package org.cloudcompare.backend.api.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> status = new HashMap<>();

        status.put("backend", "ok");

        try (Connection conn = dataSource.getConnection()) {
            status.put("db", "ok");
        } catch (Exception e) {
            status.put("db", "down");
            status.put("dbError", e.getMessage());
        }

        status.put("message", "Health check completed");
        status.put("timestamp", Instant.now().toString());

        return status;
    }
}
