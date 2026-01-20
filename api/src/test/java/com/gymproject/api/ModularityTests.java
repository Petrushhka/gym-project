package com.gymproject.api;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

public class ModularityTests {
    @Test void verifyStructure() {
        ApplicationModules.of(GymProjectApplication.class).verify();
    }
}
