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

    /**
     * Updates an existing work allocation.
     *
     * @param authUserToken the authentication token of the user
     * @param workAllocation the updated work allocation details
     * @return ApiResponse indicating the result of the operation
     */
    ApiResponse updateWorkAllocation(String authUserToken, WorkAllocationDTO workAllocation);

    /**
     * Updates an existing work order.
     *
     * @param authUserToken the authentication token of the user
     * @param workOrder the updated work order details
     * @return ApiResponse indicating the result of the operation
     */
    ApiResponse updateWorkOrder(String authUserToken, WorkOrderDTO workOrder);

    /**
     * Reads a work allocation by work order ID and user ID.
     *
     * @param authUserToken the authentication token of the user
     * @param workOrderId   the ID of the work order
     * @param userId        the ID of the user
     * @return ApiResponse containing the work allocation details or an error response
     */
    ApiResponse readWorkAllocation(String authUserToken, String workOrderId, String userId);

    /**
     * Reads a work order by its ID.
     *
     * @param authUserToken the authentication token of the user
     * @param workOrderId   the ID of the work order
     * @return ApiResponse containing the work order details or an error response
     */
    ApiResponse readWorkOrder(String authUserToken, String workOrderId);
}
