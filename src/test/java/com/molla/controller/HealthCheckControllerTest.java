package com.molla.controller;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HealthCheckControllerTest {

    @Test
    void healthzReturnsOkStatus() {
        HealthCheckController controller = new HealthCheckController();

        Map<String, String> response = controller.healthz();

        assertThat(response).containsEntry("status", "ok");
    }
}
