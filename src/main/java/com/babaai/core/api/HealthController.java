package com.babaai.core.api;

import com.babaai.core.dto.Dtos;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public Dtos.HealthResponse health() {
        return Dtos.HealthResponse.ok();
    }
}
