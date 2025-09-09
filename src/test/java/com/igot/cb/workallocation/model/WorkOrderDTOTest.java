package com.igot.cb.workallocation.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorkOrderDTOTest {

    @Test
    void testGettersAndSetters() {
        WorkOrderDTO dto = new WorkOrderDTO();

        dto.setWorkOrderId("wo-123");
        dto.setCreatedBy("creator");
        dto.setUpdatedBy("updater");
        dto.setCreatedAt("2024-06-01T10:00:00Z");
        dto.setUpdatedAt("2024-06-02T12:00:00Z");
        dto.setWorkOrderName("Test Work Order");
        dto.setPublishedOn("2024-06-01");
        dto.setPublishedBy("admin");
        dto.setWorkOrderStatus("Active");
        dto.setGeneratedWorkOrderPdfLink("http://pdf-link");
        dto.setSignedPdfLink("http://signed-pdf-link");
        dto.setUserIds(List.of("user1", "user2"));
        dto.setWorkAllocationIds(List.of("wa1", "wa2"));

        assertEquals("wo-123", dto.getWorkOrderId());
        assertEquals("creator", dto.getCreatedBy());
        assertEquals("updater", dto.getUpdatedBy());
        assertEquals("2024-06-01T10:00:00Z", dto.getCreatedAt());
        assertEquals("2024-06-02T12:00:00Z", dto.getUpdatedAt());
        assertEquals("Test Work Order", dto.getWorkOrderName());
        assertEquals("2024-06-01", dto.getPublishedOn());
        assertEquals("admin", dto.getPublishedBy());
        assertEquals("Active", dto.getWorkOrderStatus());
        assertEquals("http://pdf-link", dto.getGeneratedWorkOrderPdfLink());
        assertEquals("http://signed-pdf-link", dto.getSignedPdfLink());
        assertEquals(List.of("user1", "user2"), dto.getUserIds());
        assertEquals(List.of("wa1", "wa2"), dto.getWorkAllocationIds());
    }

    @Test
    void testDefaultValues() {
        WorkOrderDTO dto = new WorkOrderDTO();

        assertNull(dto.getWorkOrderId());
        assertNull(dto.getCreatedBy());
        assertNull(dto.getUpdatedBy());
        assertNull(dto.getCreatedAt());
        assertNull(dto.getUpdatedAt());
        assertNull(dto.getWorkOrderName());
        assertNull(dto.getPublishedOn());
        assertNull(dto.getPublishedBy());
        assertNull(dto.getWorkOrderStatus());
        assertNull(dto.getGeneratedWorkOrderPdfLink());
        assertNull(dto.getSignedPdfLink());
        assertNull(dto.getUserIds());
        assertNull(dto.getWorkAllocationIds());
    }
}