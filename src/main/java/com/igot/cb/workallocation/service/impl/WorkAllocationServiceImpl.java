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
        workAllocation.setWorkAllocationId(String.valueOf(Uuids.timeBased()));
        if (StringUtils.isEmpty(workAllocation.getWorkAllocationStatus()))
            workAllocation.setWorkAllocationStatus(Constants.DRAFT);
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put(Constants.WORK_ALLOCATION_ID, workAllocation.getWorkAllocationId());
        try {
            propertyMap.put(Constants.DATA, mapper.writeValueAsString(workAllocation));
        } catch (JsonProcessingException e) {
            logger.error("Exception occured while creating the work allocation", e);
            updateErrorDetails(response, "Exception occured while creating the work allocation", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        cassandraOperation.insertRecord(Constants.KEYSPACE_SUNBIRD, Constants.TABLE_WORK_ALLOCATION, propertyMap);
        syncWorkAllocationToElasticsearch(workAllocation);
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
        esUtilService.addDocument(cbServerProperties.getWorkOrderEntity(),
                workOrderDTO.getWorkOrderId(),
                mapper.convertValue(workOrderDTO, new TypeReference<Map<String, Object>>() {
                }), cbServerProperties.getElasticWorkOrderJsonPath());
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
     * Synchronizes the work allocation data to Elasticsearch.
     *
     * @param existingWorkAllocation The WorkAllocationDTO object containing work allocation details
     */
    private void syncWorkAllocationToElasticsearch(WorkAllocationDTO existingWorkAllocation) {
        esUtilService.addDocument(cbServerProperties.getWorkAllocationEntity(),
                existingWorkAllocation.getWorkAllocationId(),
                mapper.convertValue(existingWorkAllocation, new TypeReference<Map<String, Object>>() {
                }),
                cbServerProperties.getElasticWorkAllocationJsonPath());
    }
}
