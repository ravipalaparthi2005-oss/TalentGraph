package com.talentgraph.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealthStatus() {
        Map<String, Object> statusMap = new HashMap<>();
        statusMap.put("status", "UP");
        statusMap.put("service", "TalentGraph Backend");
        statusMap.put("version", "1.0.0");
        statusMap.put("environment", System.getProperty("spring.profiles.active", "local"));

        return ResponseEntity.ok(ApiResponse.success(statusMap, "TalentGraph service is healthy"));
    }
}
