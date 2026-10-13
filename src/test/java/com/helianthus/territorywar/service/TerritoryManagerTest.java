package com.helianthus.territorywar.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TerritoryManagerTest {
    @Test
    void getRejectsNullServer() {
        assertThrows(NullPointerException.class, () -> TerritoryManager.get(null));
    }
}
