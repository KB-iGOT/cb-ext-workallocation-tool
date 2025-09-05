package com.igot.cb.workallocation.service.impl;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.workallocation.authentication.util.AccessTokenValidator;
import com.igot.cb.workallocation.model.WorkAllocationDTO;
import com.igot.cb.workallocation.model.WorkOrderDTO;
import com.igot.cb.workallocation.service.IWorkAllocationService;
import com.igot.cb.workallocation.transactional.cassandrautils.CassandraOperation;
import com.igot.cb.workallocation.transactional.elasticsearch.service.EsUtilService;
import com.igot.cb.workallocation.util.ApiResponse;
import com.igot.cb.workallocation.util.CbServerProperties;
import com.igot.cb.workallocation.util.Constants;
import com.igot.cb.workallocation.util.ProjectUtil;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service implementation for managing work allocations and work orders.
 */
@Service
public class WorkAllocationServiceImpl implements IWorkAllocationService {

    public static final Logger logger = LoggerFactory.getLogger(WorkAllocationServiceImpl.class);


    @Autowired
    private AccessTokenValidator accessTokenValidator;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private CassandraOperation cassandraOperation;

    @Autowired
    private EsUtilService esUtilService;

    @Autowired
    private CbServerProperties cbServerProperties;

    /**
     * Creates a new work allocation.
     *
     * @param authUserToken  The authentication token of the user
     * @param workAllocation The WorkAllocationDTO object containing work allocation details
     * @return An ApiResponse indicating the result of the operation
     */
    @Override
    public ApiResponse createWorkAllocation(String authUserToken, WorkAllocationDTO workAllocation) {
        logger.info("WorkAllocationServiceImpl::createWorkAllocation started");
        ApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_CREATE_WORK_ALLOCATION);
        String userId = accessTokenValidator.fetchUserIdFromAccessToken(authUserToken);
        if (StringUtils.isEmpty(userId)) {
            updateErrorDetails(response, Constants.USER_ID_DOESNT_EXIST, HttpStatus.BAD_REQUEST);
            return response;
        }
        String errMsg = validateWorkAllocation(workAllocation, Constants.CREATE);
        if (StringUtils.isNotEmpty(errMsg)) {
            updateErrorDetails(response, errMsg, HttpStatus.BAD_REQUEST);
            return response;
        }
        if (StringUtils.isEmpty(workAllocation.getWorkAllocationStatus()))
            workAllocation.setWorkAllocationStatus(Constants.DRAFT);
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put(Constants.WORK_ORDER_ID, workAllocation.getWorkOrderId());
        propertyMap.put(Constants.USER_ID, workAllocation.getUserId());
        try {
            propertyMap.put(Constants.DATA, mapper.writeValueAsString(workAllocation));
        } catch (JsonProcessingException e) {
            logger.error("Exception occured while creating the work allocation", e);
            updateErrorDetails(response, "Exception occured while creating the work allocation", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        cassandraOperation.upsertRecord(Constants.KEYSPACE_SUNBIRD, Constants.TABLE_WORK_ALLOCATION, propertyMap);
        return response;
    }

    /**
     * Creates a new work order.
     *
     * @param authUserToken The authentication token of the user
     * @param workOrderDTO  The WorkOrderDTO object containing work order details
     * @return An ApiResponse indicating the result of the operation
     */
    @Override
    public ApiResponse createWorkOrder(String authUserToken, WorkOrderDTO workOrderDTO) {
        logger.info("WorkAllocationServiceImpl::createWorkOrder started");
        ApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_CREATE_WORK_ALLOCATION);
        String userId = accessTokenValidator.fetchUserIdFromAccessToken(authUserToken);
        if (StringUtils.isEmpty(userId)) {
            updateErrorDetails(response, Constants.USER_ID_DOESNT_EXIST, HttpStatus.BAD_REQUEST);
            return response;
        }
        String errMsg = validateWorkOrder(workOrderDTO, Constants.CREATE);
        if (StringUtils.isNotEmpty(errMsg)) {
            updateErrorDetails(response, errMsg, HttpStatus.BAD_REQUEST);
            return response;
        }
        workOrderDTO.setWorkOrderId(String.valueOf(Uuids.timeBased()));
        workOrderDTO.setCreatedBy(userId);
        workOrderDTO.setCreatedAt(getFormattedCurrentTime(new Timestamp(System.currentTimeMillis())));
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put(Constants.WORK_ORDER_ID, workOrderDTO.getWorkOrderId());
        try {
            propertyMap.put(Constants.DATA, mapper.writeValueAsString(workOrderDTO));
        } catch (JsonProcessingException e) {
            logger.error("Exception occured while creating the work order", e);
            updateErrorDetails(response, "Exception occured while creating the work order", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (StringUtils.isEmpty(workOrderDTO.getWorkOrderStatus())) workOrderDTO.setWorkOrderStatus(Constants.DRAFT);
        cassandraOperation.insertRecord(Constants.KEYSPACE_SUNBIRD, Constants.TABLE_WORK_ORDER, propertyMap);
        syncWorkOrderToElasticsearch(workOrderDTO);
        return response;
    }

    /**
     * Validates the WorkOrderDTO object based on the specified type.
     *
     * @param workOrderDTO The WorkOrderDTO object to validate
     * @param type         The type of operation (e.g., "CREATE")
     * @return An error message if validation fails, otherwise null
     */
    private String validateWorkOrder(WorkOrderDTO workOrderDTO, String type) {
        if (Constants.CREATE.equalsIgnoreCase(type)) {
            if (CollectionUtils.isEmpty(workOrderDTO.getUserIds())) return "User Ids is required";
            if (StringUtils.isEmpty(workOrderDTO.getWorkOrderName())) return "Work Order name is required";
        }
        return null;
    }

    /**
     * Validates the WorkAllocationDTO object based on the specified type.
     *
     * @param workAllocation The WorkAllocationDTO object to validate
     * @param type           The type of operation (e.g., "CREATE")
     * @return An error message if validation fails, otherwise null
     */
    private String validateWorkAllocation(WorkAllocationDTO workAllocation, String type) {
        if (Constants.CREATE.equalsIgnoreCase(type)) {
            if (StringUtils.isEmpty(workAllocation.getWorkOrderId())) return "Work Order Id is required";
            if (StringUtils.isEmpty(workAllocation.getDesignation())) return "Designation is required";
        }
        return null;
    }


    /**
     * Updates the error details in the ApiResponse object.
     *
     * @param response     The ApiResponse object to update
     * @param errorMessage The error message to set
     * @param status       The HTTP status code to set
     */
    public void updateErrorDetails(ApiResponse response, String errorMessage, HttpStatus status) {
        response.getParams().setErrMsg(errorMessage);
        response.getParams().setStatus(Constants.FAILED);
        response.setResponseCode(status);
    }

    /**
     * Formats the current time to a specific string format.
     *
     * @param currentTime The current time as a Timestamp object
     * @return The formatted current time as a string
     */
    private String getFormattedCurrentTime(Timestamp currentTime) {
        ZonedDateTime zonedDateTime = currentTime.toInstant().atZone(ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.TIME_FORMAT);
        return zonedDateTime.format(formatter);
    }

    /**
     * Updates an existing work allocation.
     *
     * @param authUserToken  The authentication token of the user
     * @param workAllocation The WorkAllocationDTO object containing updated work allocation details
     * @return An ApiResponse indicating the result of the operation
     */
    @Override
    public ApiResponse updateWorkAllocation(String authUserToken, WorkAllocationDTO workAllocation) {
        logger.info("WorkAllocationServiceImpl::updateWorkAllocation started");
        ApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_UPDATE_WORK_ALLOCATION);
        String userId = accessTokenValidator.fetchUserIdFromAccessToken(authUserToken);
        if (StringUtils.isEmpty(userId)) {
            updateErrorDetails(response, Constants.USER_ID_DOESNT_EXIST, HttpStatus.BAD_REQUEST);
            return response;
        }
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put(Constants.WORK_ORDER_ID, workAllocation.getWorkOrderId());
        propertyMap.put(Constants.USER_ID, workAllocation.getUserId());
        List<Map<String, Object>> workAllocationDetailsList = cassandraOperation.getRecordsByPropertiesByKey(Constants.KEYSPACE_SUNBIRD,
                Constants.TABLE_WORK_ALLOCATION, propertyMap, null, null);
        Map<String, Object> workAllocationDetailsMap = workAllocationDetailsList.get(0);
        if (MapUtils.isEmpty(workAllocationDetailsMap)) {
            updateErrorDetails(response, "No work allocation record found for the given id", HttpStatus.BAD_REQUEST);
            return response;
        }
        String existingData = (String) workAllocationDetailsMap.get(Constants.DATA);
        WorkAllocationDTO existingWorkAllocation = null;
        try {
            existingWorkAllocation = mapper.readValue(existingData, WorkAllocationDTO.class);
        } catch (JsonProcessingException e) {
            logger.error("Exception occured while parsing existing work allocation data", e);
            updateErrorDetails(response, "Exception occured while parsing existing work allocation data", HttpStatus.INTERNAL_SERVER_ERROR);
            return response;
        }
        updateWorkAllocationFields(workAllocation, existingWorkAllocation);
        updateWorkAllocationInCassandra(existingWorkAllocation, response);
        return response;
    }


    /**
     * Updates the work allocation record in Cassandra.
     *
     * @param existingWorkAllocation The existing WorkAllocationDTO object
     * @param response               The ApiResponse object to update in case of errors
     */
    private void updateWorkAllocationInCassandra(WorkAllocationDTO existingWorkAllocation, ApiResponse response) {
        Map<String, Object> updatePropertyMap = new HashMap<>();
        try {
            updatePropertyMap.put(Constants.DATA, mapper.writeValueAsString(existingWorkAllocation));
            updatePropertyMap.put(Constants.WORK_ORDER_ID, existingWorkAllocation.getWorkOrderId());
            updatePropertyMap.put(Constants.USER_ID, existingWorkAllocation.getUserId());
        } catch (JsonProcessingException e) {
            logger.error("Exception occured while updating the work allocation", e);
            updateErrorDetails(response, "Exception occured while updating the work allocation", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        cassandraOperation.upsertRecord(Constants.KEYSPACE_SUNBIRD, Constants.TABLE_WORK_ALLOCATION, updatePropertyMap);

    }

    /**
     * Updates the fields of the existing WorkAllocationDTO object with the values from the new WorkAllocationDTO object.
     *
     * @param workAllocation         The new WorkAllocationDTO object containing updated values
     * @param existingWorkAllocation The existing WorkAllocationDTO object to be updated
     */
    private static void updateWorkAllocationFields(WorkAllocationDTO workAllocation, WorkAllocationDTO existingWorkAllocation) {
        if (StringUtils.isNotEmpty(workAllocation.getWorkOrderId())) {
            existingWorkAllocation.setWorkOrderId(workAllocation.getWorkOrderId());
        }
        if (StringUtils.isNotEmpty(workAllocation.getDesignation())) {
            existingWorkAllocation.setDesignation(workAllocation.getDesignation());
        }
        if (StringUtils.isNotEmpty(workAllocation.getUserId())) {
            existingWorkAllocation.setUserId(workAllocation.getUserId());
        }
        if (StringUtils.isNotEmpty(workAllocation.getUserName())) {
            existingWorkAllocation.setUserName(workAllocation.getUserName());
        }
        if (StringUtils.isNotEmpty(workAllocation.getDescription())) {
            existingWorkAllocation.setDescription(workAllocation.getDescription());
        }
        if (StringUtils.isNotEmpty(workAllocation.getWorkAllocationStatus())) {
            existingWorkAllocation.setWorkAllocationStatus(workAllocation.getWorkAllocationStatus());
        }
        if (!CollectionUtils.isEmpty(workAllocation.getRoleActivitiesDetails())) {
            existingWorkAllocation.setRoleActivitiesDetails(workAllocation.getRoleActivitiesDetails());
        }
        if (!CollectionUtils.isEmpty(workAllocation.getCompetenciesV6())) {
            existingWorkAllocation.setCompetenciesV6(workAllocation.getCompetenciesV6());
        }
    }


    /**
     * Updates an existing work order.
     *
     * @param authUserToken The authentication token of the user
     * @param workOrder     The WorkOrderDTO object containing updated work order details
     * @return An ApiResponse indicating the result of the operation
     */
    @Override
    public ApiResponse updateWorkOrder(String authUserToken, WorkOrderDTO workOrder) {
        logger.info("WorkAllocationServiceImpl::updateWorkOrder started");
        ApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_UPDATE_WORK_ORDER);
        String userId = accessTokenValidator.fetchUserIdFromAccessToken(authUserToken);
        if (StringUtils.isEmpty(userId)) {
            updateErrorDetails(response, Constants.USER_ID_DOESNT_EXIST, HttpStatus.BAD_REQUEST);
            return response;
        }
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put(Constants.WORK_ORDER_ID, workOrder.getWorkOrderId());
        List<Map<String, Object>> workOrderDetailsList = cassandraOperation.getRecordsByPropertiesByKey(Constants.KEYSPACE_SUNBIRD,
                Constants.TABLE_WORK_ORDER, propertyMap, null, null);
        if (CollectionUtils.isEmpty(workOrderDetailsList)) {
            updateErrorDetails(response, "No work order record found for the given id", HttpStatus.BAD_REQUEST);
            return response;
        }
        Map<String, Object> workOrderDetailsMap = workOrderDetailsList.get(0);
        if (MapUtils.isEmpty(workOrderDetailsMap)) {
            updateErrorDetails(response, "No work order record found for the given id", HttpStatus.BAD_REQUEST);
            return response;
        }
        String existingData = (String) workOrderDetailsMap.get(Constants.DATA);
        WorkOrderDTO existingWorkOrder = parseExistingWorkOrder(existingData, response);
        if (existingWorkOrder == null) {
            return response;
        }
        updateWorkOrderFields(workOrder, existingWorkOrder, userId);
        updateWorkOrderInCassandra(workOrder, existingWorkOrder, response);
        syncWorkOrderToElasticsearch(existingWorkOrder);
        return response;
    }

    /**
     * Parses existing work order data from JSON string.
     *
     * @param existingData The JSON string containing work order data
     * @param response     The ApiResponse object to update in case of errors
     * @return The parsed WorkOrderDTO object or null if parsing fails
     */
    private WorkOrderDTO parseExistingWorkOrder(String existingData, ApiResponse response) {
        try {
            return mapper.readValue(existingData, WorkOrderDTO.class);
        } catch (JsonProcessingException e) {
            logger.error("Exception occurred while parsing existing work order data", e);
            updateErrorDetails(response, "Exception occurred while parsing existing work order data", HttpStatus.INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    /**
     * Updates the work order record in Cassandra.
     *
     * @param workOrder         The WorkOrderDTO object containing updated work order details
     * @param existingWorkOrder The existing WorkOrderDTO object
     * @param response          The ApiResponse object to update in case of errors
     */
    private void updateWorkOrderInCassandra(WorkOrderDTO workOrder, WorkOrderDTO existingWorkOrder, ApiResponse response) {
        Map<String, Object> updatePropertyMap = new HashMap<>();
        try {
            updatePropertyMap.put(Constants.DATA, mapper.writeValueAsString(existingWorkOrder));
        } catch (JsonProcessingException e) {
            logger.error("Exception occurred while updating the work order", e);
            updateErrorDetails(response, "Exception occurred while updating the work order", HttpStatus.INTERNAL_SERVER_ERROR);
            return;
        }
        cassandraOperation.updateRecordByCompositeKey(Constants.KEYSPACE_SUNBIRD, Constants.TABLE_WORK_ORDER, updatePropertyMap,
                Map.of(Constants.WORK_ORDER_ID, workOrder.getWorkOrderId()));
    }

    /**
     * Synchronizes the work order data to Elasticsearch.
     *
     * @param existingWorkOrder The WorkOrderDTO object containing work order details
     */
    private void syncWorkOrderToElasticsearch(WorkOrderDTO existingWorkOrder) {
        esUtilService.addDocument(cbServerProperties.getWorkOrderEntity(),
                existingWorkOrder.getWorkOrderId(),
                mapper.convertValue(existingWorkOrder, new TypeReference<Map<String, Object>>() {
                }),
                cbServerProperties.getElasticWorkOrderJsonPath());
    }

    /**
     * Updates the fields of the existing WorkOrderDTO object with the values from the new WorkOrderDTO object.
     *
     * @param workOrder         The new WorkOrderDTO object containing updated values
     * @param existingWorkOrder The existing WorkOrderDTO object to be updated
     * @param userId            The user ID performing the update
     */
    private void updateWorkOrderFields(WorkOrderDTO workOrder, WorkOrderDTO existingWorkOrder, String userId) {
        if (StringUtils.isNotEmpty(workOrder.getWorkOrderName())) {
            existingWorkOrder.setWorkOrderName(workOrder.getWorkOrderName());
        }
        if (StringUtils.isNotEmpty(workOrder.getWorkOrderStatus())) {
            existingWorkOrder.setWorkOrderStatus(workOrder.getWorkOrderStatus());
        }
        if (StringUtils.isNotEmpty(workOrder.getPublishedOn())) {
            existingWorkOrder.setPublishedOn(workOrder.getPublishedOn());
        }
        if (StringUtils.isNotEmpty(workOrder.getPublishedBy())) {
            existingWorkOrder.setPublishedBy(workOrder.getPublishedBy());
        }
        if (StringUtils.isNotEmpty(workOrder.getGeneratedWorkOrderPdfLink())) {
            existingWorkOrder.setGeneratedWorkOrderPdfLink(workOrder.getGeneratedWorkOrderPdfLink());
        }
        if (StringUtils.isNotEmpty(workOrder.getSignedPdfLink())) {
            existingWorkOrder.setSignedPdfLink(workOrder.getSignedPdfLink());
        }
        if (!CollectionUtils.isEmpty(workOrder.getUserIds())) {
            existingWorkOrder.setUserIds(workOrder.getUserIds());
        }
        if (!CollectionUtils.isEmpty(workOrder.getWorkAllocationIds())) {
            existingWorkOrder.setWorkAllocationIds(workOrder.getWorkAllocationIds());
        }
        existingWorkOrder.setUpdatedBy(userId);
        existingWorkOrder.setUpdatedAt(getFormattedCurrentTime(new Timestamp(System.currentTimeMillis())));
    }
}
