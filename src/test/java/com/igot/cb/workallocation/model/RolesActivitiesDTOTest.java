
package com.igot.cb.workallocation.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RolesActivitiesDTOTest {

    @Test
    void testGettersAndSetters() {
        RolesActivitiesDTO dto = new RolesActivitiesDTO();

        dto.setType("roleType");
        dto.setDescription("desc");
        dto.setActivities(List.of("act1", "act2"));
        dto.setSubmissionUserId("user123");
        dto.setSubmissionUserName("User Name");

        assertEquals("roleType", dto.getType());
        assertEquals("desc", dto.getDescription());
        assertEquals(List.of("act1", "act2"), dto.getActivities());
        assertEquals("user123", dto.getSubmissionUserId());
        assertEquals("User Name", dto.getSubmissionUserName());
    }

    @Test
    void testDefaultValues() {
        RolesActivitiesDTO dto = new RolesActivitiesDTO();

        assertNull(dto.getType());
        assertNull(dto.getDescription());
        assertNull(dto.getActivities());
        assertNull(dto.getSubmissionUserId());
        assertNull(dto.getSubmissionUserName());
    }
}