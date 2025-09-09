package com.igot.cb.workallocation.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkAllocationDTOTest {

    @Test
    void testGettersAndSetters() {
        WorkAllocationDTO dto = new WorkAllocationDTO();

        dto.setDesignation("Manager");
        dto.setUserId("user123");
        dto.setUserName("John Doe");
        dto.setDescription("Test description");
        dto.setWorkOrderId("wo-001");
        RolesActivitiesDTO roleActivity = new RolesActivitiesDTO();
        dto.setRoleActivitiesDetails(List.of(roleActivity));
        Map<String, Object> competency = Map.of("key", "value");
        dto.setCompetenciesV6(List.of(competency));
        dto.setWorkAllocationStatus("Active");

        assertEquals("Manager", dto.getDesignation());
        assertEquals("user123", dto.getUserId());
        assertEquals("John Doe", dto.getUserName());
        assertEquals("Test description", dto.getDescription());
        assertEquals("wo-001", dto.getWorkOrderId());
        assertEquals(List.of(roleActivity), dto.getRoleActivitiesDetails());
        assertEquals(List.of(competency), dto.getCompetenciesV6());
        assertEquals("Active", dto.getWorkAllocationStatus());
    }

    @Test
    void testDefaultValues() {
        WorkAllocationDTO dto = new WorkAllocationDTO();

        assertNull(dto.getDesignation());
        assertNull(dto.getUserId());
        assertNull(dto.getUserName());
        assertNull(dto.getDescription());
        assertNull(dto.getWorkOrderId());
        assertNull(dto.getRoleActivitiesDetails());
        assertNull(dto.getCompetenciesV6());
        assertNull(dto.getWorkAllocationStatus());
    }
}