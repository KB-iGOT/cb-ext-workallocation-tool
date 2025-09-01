package com.igot.cb.workallocation.service;

import com.igot.cb.workallocation.model.WorkAllocationDTO;
import com.igot.cb.workallocation.model.WorkOrderDTO;
import com.igot.cb.workallocation.util.ApiResponse;

/**
 * Service interface for managing work allocations and work orders.
 */
public interface IWorkAllocationService {

    /**
     * Creates a new work allocation.
     *
     * @param authUserToken  the authentication token of the user
     * @param workAllocation the work allocation details
     * @return ApiResponse indicating the result of the operation
     */
    ApiResponse createWorkAllocation(String authUserToken, WorkAllocationDTO workAllocation);

    /**
     * Creates a new work order.
     *
     * @param authUserToken the authentication token of the user
     * @param workOrderDTO  the work order details
     * @return ApiResponse indicating the result of the operation
     */
    ApiResponse createWorkOrder(String authUserToken, WorkOrderDTO workOrderDTO);

}
