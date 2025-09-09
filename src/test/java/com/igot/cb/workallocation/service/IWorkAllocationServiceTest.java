package com.igot.cb.workallocation.service;

import com.igot.cb.workallocation.model.WorkAllocationDTO;
import com.igot.cb.workallocation.model.WorkOrderDTO;
import com.igot.cb.workallocation.service.IWorkAllocationService;
import com.igot.cb.workallocation.util.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class IWorkAllocationServiceTest {

    private IWorkAllocationService service;

    @BeforeEach
    void setUp() {
        service = Mockito.mock(IWorkAllocationService.class);
    }

    @Test
    void testCreateWorkAllocation() {
        WorkAllocationDTO dto = new WorkAllocationDTO();
        ApiResponse response = new ApiResponse();
        when(service.createWorkAllocation(anyString(), any(WorkAllocationDTO.class))).thenReturn(response);

        ApiResponse result = service.createWorkAllocation("token", dto);

        assertSame(response, result);
        verify(service).createWorkAllocation("token", dto);
    }

    @Test
    void testCreateWorkOrder() {
        WorkOrderDTO dto = new WorkOrderDTO();
        ApiResponse response = new ApiResponse();
        when(service.createWorkOrder(anyString(), any(WorkOrderDTO.class))).thenReturn(response);

        ApiResponse result = service.createWorkOrder("token", dto);

        assertSame(response, result);
        verify(service).createWorkOrder("token", dto);
    }

    @Test
    void testUpdateWorkAllocation() {
        WorkAllocationDTO dto = new WorkAllocationDTO();
        ApiResponse response = new ApiResponse();
        when(service.updateWorkAllocation(anyString(), any(WorkAllocationDTO.class))).thenReturn(response);

        ApiResponse result = service.updateWorkAllocation("token", dto);

        assertSame(response, result);
        verify(service).updateWorkAllocation("token", dto);
    }

    @Test
    void testUpdateWorkOrder() {
        WorkOrderDTO dto = new WorkOrderDTO();
        ApiResponse response = new ApiResponse();
        when(service.updateWorkOrder(anyString(), any(WorkOrderDTO.class))).thenReturn(response);

        ApiResponse result = service.updateWorkOrder("token", dto);

        assertSame(response, result);
        verify(service).updateWorkOrder("token", dto);
    }

    @Test
    void testReadWorkAllocation() {
        ApiResponse response = new ApiResponse();
        when(service.readWorkAllocation(anyString(), anyString(), anyString())).thenReturn(response);

        ApiResponse result = service.readWorkAllocation("token", "woId", "userId");

        assertSame(response, result);
        verify(service).readWorkAllocation("token", "woId", "userId");
    }

    @Test
    void testReadWorkOrder() {
        ApiResponse response = new ApiResponse();
        when(service.readWorkOrder(anyString(), anyString())).thenReturn(response);

        ApiResponse result = service.readWorkOrder("token", "woId");

        assertSame(response, result);
        verify(service).readWorkOrder("token", "woId");
    }
}