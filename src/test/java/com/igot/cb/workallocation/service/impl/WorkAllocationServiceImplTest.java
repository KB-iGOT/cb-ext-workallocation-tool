package com.igot.cb.workallocation.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.workallocation.authentication.util.AccessTokenValidator;
import com.igot.cb.workallocation.model.RolesActivitiesDTO;
import com.igot.cb.workallocation.model.WorkAllocationDTO;
import com.igot.cb.workallocation.model.WorkOrderDTO;
import com.igot.cb.workallocation.transactional.cassandrautils.CassandraOperation;
import com.igot.cb.workallocation.transactional.elasticsearch.service.EsUtilService;
import com.igot.cb.workallocation.util.ApiResponse;
import com.igot.cb.workallocation.util.CbServerProperties;
import com.igot.cb.workallocation.util.Constants;
import com.igot.cb.workallocation.util.ProjectUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkAllocationServiceImplTest {

    @InjectMocks
    private WorkAllocationServiceImpl service;

    @Mock
    private AccessTokenValidator accessTokenValidator;
    @Mock
    private ObjectMapper mapper;
    @Mock
    private CassandraOperation cassandraOperation;
    @Mock
    private EsUtilService esUtilService;
    @Mock
    private CbServerProperties cbServerProperties;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateWorkAllocation_Success() throws Exception {
        String token = "token";
        String userId = "user1";
        WorkAllocationDTO dto = new WorkAllocationDTO();
        dto.setWorkOrderId("wo1");
        dto.setDesignation("desig");
        dto.setUserId("user1");
        when(accessTokenValidator.fetchUserIdFromAccessToken(token)).thenReturn(userId);
        when(mapper.writeValueAsString(any())).thenReturn("{}");
        ApiResponse response = service.createWorkAllocation(token, dto);
        assertEquals(HttpStatus.OK, response.getResponseCode());
        verify(cassandraOperation).upsertRecord(any(), any(), any());
    }

    @Test
    void testCreateWorkAllocation_InvalidUser() {
        String token = "token";
        WorkAllocationDTO dto = new WorkAllocationDTO();
        when(accessTokenValidator.fetchUserIdFromAccessToken(token)).thenReturn("");
        ApiResponse response = service.createWorkAllocation(token, dto);
        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
    }

    @Test
    void testCreateWorkAllocation_ValidationError() {
        String token = "token";
        String userId = "user1";
        WorkAllocationDTO dto = new WorkAllocationDTO(); // missing required fields
        when(accessTokenValidator.fetchUserIdFromAccessToken(token)).thenReturn(userId);
        ApiResponse response = service.createWorkAllocation(token, dto);
        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
    }

    @Test
    void testCreateWorkAllocation_JsonProcessingException() throws Exception {
        String token = "token";
        String userId = "user1";
        WorkAllocationDTO dto = new WorkAllocationDTO();
        dto.setWorkOrderId("wo1");
        dto.setDesignation("desig");
        dto.setUserId("user1");
        when(accessTokenValidator.fetchUserIdFromAccessToken(token)).thenReturn(userId);
        when(mapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("error") {});
        ApiResponse response = service.createWorkAllocation(token, dto);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
    }

    @Test
    void testCreateWorkOrder_Success() throws Exception {
        String token = "token";
        String userId = "user123";
        WorkOrderDTO dto = new WorkOrderDTO();
        dto.setWorkOrderName("Test Order");
        dto.setUserIds(List.of("u1", "u2"));
        when(accessTokenValidator.fetchUserIdFromAccessToken(token)).thenReturn(userId);
        when(mapper.writeValueAsString(any())).thenReturn("{}");
        when(cbServerProperties.getWorkOrderEntity()).thenReturn("workorder");
        when(cbServerProperties.getElasticWorkOrderJsonPath()).thenReturn("path");
        ApiResponse response = service.createWorkOrder(token, dto);
        assertEquals(HttpStatus.OK, response.getResponseCode());
        verify(cassandraOperation).insertRecord(eq(Constants.KEYSPACE_SUNBIRD), eq(Constants.TABLE_WORK_ORDER), any());
        verify(esUtilService).addDocument(any(), any(), any(), any());
    }

    @Test
    void testCreateWorkOrder_InvalidUser() {
        String token = "token";
        WorkOrderDTO dto = new WorkOrderDTO();
        when(accessTokenValidator.fetchUserIdFromAccessToken(token)).thenReturn("");
        ApiResponse response = service.createWorkOrder(token, dto);
        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
    }

    @Test
    void testCreateWorkOrder_ValidationError() {
        String token = "token";
        String userId = "user1";
        WorkOrderDTO dto = new WorkOrderDTO();
        when(accessTokenValidator.fetchUserIdFromAccessToken(token)).thenReturn(userId);
        ApiResponse response = service.createWorkOrder(token, dto);
        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
    }

    @Test
    void testCreateWorkOrder_JsonProcessingException() throws Exception {
        String token = "token";
        String userId = "user1";
        WorkOrderDTO dto = new WorkOrderDTO();
        dto.setWorkOrderName("Test");
        dto.setUserIds(List.of("u1"));
        when(accessTokenValidator.fetchUserIdFromAccessToken(token)).thenReturn(userId);
        when(mapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("error") {});
        ApiResponse response = service.createWorkOrder(token, dto);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
    }

    @Test
    void testUpdateWorkAllocation_Success() throws Exception {
        String token = "token";
        String userId = "user1";
        WorkAllocationDTO dto = new WorkAllocationDTO();
        dto.setWorkOrderId("wo1");
        dto.setUserId("user1");
        Map<String, Object> record = new HashMap<>();
        record.put(Constants.DATA, "{}");
        List<Map<String, Object>> records = List.of(record);
        when(accessTokenValidator.fetchUserIdFromAccessToken(token)).thenReturn(userId);
        when(cassandraOperation.getRecordsByPropertiesByKey(any(), any(), any(), any(), any())).thenReturn(records);
        when(mapper.readValue(anyString(), eq(WorkAllocationDTO.class))).thenReturn(new WorkAllocationDTO());
        when(mapper.writeValueAsString(any())).thenReturn("{}");
        ApiResponse response = service.updateWorkAllocation(token, dto);
        assertEquals(HttpStatus.OK, response.getResponseCode());
        verify(cassandraOperation).upsertRecord(any(), any(), any());
    }

    @Test
    void testUpdateWorkAllocation_InvalidUser() {
        String token = "token";
        WorkAllocationDTO dto = new WorkAllocationDTO();
        when(accessTokenValidator.fetchUserIdFromAccessToken(token)).thenReturn("");
        ApiResponse response = service.updateWorkAllocation(token, dto);
        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
    }

    @Test
    void testUpdateWorkAllocation_NoRecordFound() {
        String token = "token";
        String userId = "user1";
        WorkAllocationDTO dto = new WorkAllocationDTO();
        dto.setWorkOrderId("wo1");
        dto.setUserId("user1");
        when(accessTokenValidator.fetchUserIdFromAccessToken(token)).thenReturn(userId);
        when(cassandraOperation.getRecordsByPropertiesByKey(any(), any(), any(), any(), any())).thenReturn(List.of(new HashMap<>()));
        ApiResponse response = service.updateWorkAllocation(token, dto);
        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
    }

    @Test
    void testUpdateWorkAllocation_JsonProcessingException() throws Exception {
        String token = "token";
        String userId = "user1";
        WorkAllocationDTO dto = new WorkAllocationDTO();
        dto.setWorkOrderId("wo1");
        dto.setUserId("user1");
        Map<String, Object> record = new HashMap<>();
        record.put(Constants.DATA, "{}");
        List<Map<String, Object>> records = List.of(record);
        when(accessTokenValidator.fetchUserIdFromAccessToken(token)).thenReturn(userId);
        when(cassandraOperation.getRecordsByPropertiesByKey(any(), any(), any(), any(), any())).thenReturn(records);
        when(mapper.readValue(anyString(), eq(WorkAllocationDTO.class))).thenThrow(new JsonProcessingException("error") {});
        ApiResponse response = service.updateWorkAllocation(token, dto);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
    }

    @Test
    void testUpdateWorkOrder_Success() throws Exception {
        String token = "token";
        String userId = "user1";
        WorkOrderDTO dto = new WorkOrderDTO();
        dto.setWorkOrderId("wo1");
        Map<String, Object> record = new HashMap<>();
        record.put(Constants.DATA, "{}");
        List<Map<String, Object>> records = List.of(record);
        when(accessTokenValidator.fetchUserIdFromAccessToken(token)).thenReturn(userId);
        when(cassandraOperation.getRecordsByPropertiesByKey(any(), any(), any(), any(), any())).thenReturn(records);
        when(mapper.readValue(anyString(), eq(WorkOrderDTO.class))).thenReturn(new WorkOrderDTO());
        when(mapper.writeValueAsString(any())).thenReturn("{}");
        ApiResponse response = service.updateWorkOrder(token, dto);
        assertEquals(HttpStatus.OK, response.getResponseCode());
        verify(cassandraOperation).updateRecordByCompositeKey(any(), any(), any(), any());
        verify(esUtilService).addDocument(any(), any(), any(), any());
    }

    @Test
    void testUpdateWorkOrder_InvalidUser() {
        String token = "token";
        WorkOrderDTO dto = new WorkOrderDTO();
        when(accessTokenValidator.fetchUserIdFromAccessToken(token)).thenReturn("");

        ApiResponse response = service.updateWorkOrder(token, dto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
    }

    @Test
    void testUpdateWorkOrder_NoRecordFound() {
        String token = "token";
        String userId = "user1";
        WorkOrderDTO dto = new WorkOrderDTO();
        dto.setWorkOrderId("wo1");
        when(accessTokenValidator.fetchUserIdFromAccessToken(token)).thenReturn(userId);
        when(cassandraOperation.getRecordsByPropertiesByKey(any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());
        ApiResponse response = service.updateWorkOrder(token, dto);
        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
    }

    @Test
    void testUpdateWorkOrder_JsonProcessingException() throws Exception {
        String token = "token";
        String userId = "user1";
        WorkOrderDTO dto = new WorkOrderDTO();
        dto.setWorkOrderId("wo1");
        Map<String, Object> record = new HashMap<>();
        record.put(Constants.DATA, "{}");
        List<Map<String, Object>> records = List.of(record);
        when(accessTokenValidator.fetchUserIdFromAccessToken(token)).thenReturn(userId);
        when(cassandraOperation.getRecordsByPropertiesByKey(any(), any(), any(), any(), any())).thenReturn(records);
        when(mapper.readValue(anyString(), eq(WorkOrderDTO.class))).thenThrow(new JsonProcessingException("error") {});
        ApiResponse response = service.updateWorkOrder(token, dto);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
    }

    @Test
    void testReadWorkAllocation_Success() throws Exception {
        String token = "token";
        String userId = "user1";
        String workOrderId = "wo1";
        String userIdParam = "user1";
        Map<String, Object> record = new HashMap<>();
        record.put(Constants.DATA, "{}");
        List<Map<String, Object>> records = List.of(record);
        when(accessTokenValidator.fetchUserIdFromAccessToken(token)).thenReturn(userId);
        when(cassandraOperation.getRecordsByPropertiesByKey(any(), any(), any(), any(), any())).thenReturn(records);
        when(mapper.readValue(anyString(), any(Class.class))).thenReturn(new HashMap<>());
        ApiResponse response = service.readWorkAllocation(token, workOrderId, userIdParam);
        assertEquals(HttpStatus.OK, response.getResponseCode());
    }

    @Test
    void testReadWorkAllocation_InvalidUser() {
        String token = "token";
        String workOrderId = "wo1";
        String userIdParam = "user1";
        when(accessTokenValidator.fetchUserIdFromAccessToken(token)).thenReturn("");
        ApiResponse response = service.readWorkAllocation(token, workOrderId, userIdParam);
        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
    }

    @Test
    void testReadWorkAllocation_MissingParams() {
        String token = "token";
        String userId = "user1";
        when(accessTokenValidator.fetchUserIdFromAccessToken(token)).thenReturn(userId);
        ApiResponse response = service.readWorkAllocation(token, "", "");
        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
    }

    @Test
    void testReadWorkAllocation_NotFound() {
        String token = "token";
        String userId = "user1";
        String workOrderId = "wo1";
        String userIdParam = "user1";
        when(accessTokenValidator.fetchUserIdFromAccessToken(token)).thenReturn(userId);
        when(cassandraOperation.getRecordsByPropertiesByKey(any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());
        ApiResponse response = service.readWorkAllocation(token, workOrderId, userIdParam);
        assertEquals(HttpStatus.NOT_FOUND, response.getResponseCode());
    }

    @Test
    void testReadWorkAllocation_JsonProcessingException() throws Exception {
        String token = "token";
        String userId = "user1";
        String workOrderId = "wo1";
        String userIdParam = "user1";
        Map<String, Object> record = new HashMap<>();
        record.put(Constants.DATA, "{}");
        List<Map<String, Object>> records = List.of(record);
        when(accessTokenValidator.fetchUserIdFromAccessToken(token)).thenReturn(userId);
        when(cassandraOperation.getRecordsByPropertiesByKey(any(), any(), any(), any(), any())).thenReturn(records);
        when(mapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenThrow(new JsonProcessingException("error") {});
        ApiResponse response = service.readWorkAllocation(token, workOrderId, userIdParam);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
    }

    @Test
    void testReadWorkOrder_Success() throws Exception {
        String token = "token";
        String userId = "user1";
        String workOrderId = "wo1";
        Map<String, Object> record = new HashMap<>();
        record.put(Constants.DATA, "{}");
        List<Map<String, Object>> records = List.of(record);
        when(accessTokenValidator.fetchUserIdFromAccessToken(token)).thenReturn(userId);
        when(cassandraOperation.getRecordsByPropertiesByKey(any(), any(), any(), any(), any())).thenReturn(records);
        when(mapper.readValue(anyString(), any(Class.class))).thenReturn(new HashMap<>());
        ApiResponse response = service.readWorkOrder(token, workOrderId);
        assertEquals(HttpStatus.OK, response.getResponseCode());
    }

    @Test
    void testReadWorkOrder_InvalidUser() {
        String token = "token";
        String workOrderId = "wo1";
        when(accessTokenValidator.fetchUserIdFromAccessToken(token)).thenReturn("");
        ApiResponse response = service.readWorkOrder(token, workOrderId);
        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
    }

    @Test
    void testReadWorkOrder_NotFound() {
        String token = "token";
        String userId = "user1";
        String workOrderId = "wo1";
        when(accessTokenValidator.fetchUserIdFromAccessToken(token)).thenReturn(userId);
        when(cassandraOperation.getRecordsByPropertiesByKey(any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());
        ApiResponse response = service.readWorkOrder(token, workOrderId);
        assertEquals(HttpStatus.NOT_FOUND, response.getResponseCode());
    }

    @Test
    void testReadWorkOrder_Exception() throws Exception {
        String token = "token";
        String userId = "user1";
        String workOrderId = "wo1";
        when(accessTokenValidator.fetchUserIdFromAccessToken(token)).thenReturn(userId);
        when(cassandraOperation.getRecordsByPropertiesByKey(any(), any(), any(), any(), any())).thenThrow(new RuntimeException("error"));
        ApiResponse response = service.readWorkOrder(token, workOrderId);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
    }

    @Test
    void testCreateWorkAllocation_JsonProcessingException_ReturnsImmediately() throws Exception {
        String token = "token";
        String userId = "user1";
        WorkAllocationDTO dto = new WorkAllocationDTO();
        dto.setWorkOrderId("wo1");
        dto.setDesignation("desig");
        dto.setUserId("user1");
        when(accessTokenValidator.fetchUserIdFromAccessToken(token)).thenReturn(userId);
        when(mapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("error") {});
        ApiResponse response = service.createWorkAllocation(token, dto);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
    }

    @Test
    void testUpdateWorkAllocationInCassandra_JsonProcessingException() throws Exception {
        WorkAllocationDTO existing = new WorkAllocationDTO();
        existing.setWorkOrderId("wo1");
        existing.setUserId("user1");
        ApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_UPDATE_WORK_ALLOCATION);
        doThrow(new JsonProcessingException("error") {}).when(mapper).writeValueAsString(any());
        Method m = WorkAllocationServiceImpl.class.getDeclaredMethod("updateWorkAllocationInCassandra", WorkAllocationDTO.class, ApiResponse.class);
        m.setAccessible(true);
        m.invoke(service, existing, response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
    }

    @Test
    void testSyncWorkOrderToElasticsearch_Exception() {
        WorkOrderDTO workOrder = new WorkOrderDTO();
        workOrder.setWorkOrderId("wo1");

        when(cbServerProperties.getWorkOrderEntity()).thenReturn("workorder");
        when(cbServerProperties.getElasticWorkOrderJsonPath()).thenReturn("path");
        when(mapper.convertValue(any(), any(TypeReference.class))).thenThrow(new RuntimeException("error"));
        try {
            Method m = WorkAllocationServiceImpl.class.getDeclaredMethod("syncWorkOrderToElasticsearch", WorkOrderDTO.class);
            m.setAccessible(true);
            m.invoke(service, workOrder);
        } catch (Exception e) {
            // Expected exception, just for coverage
        }
    }

    @Test
    void testValidateWorkOrder_AllBranches() {
        WorkOrderDTO dto = new WorkOrderDTO();
        assertEquals("User Ids is required", invokeValidateWorkOrder(dto, Constants.CREATE));
        dto.setUserIds(List.of("u1"));
        assertEquals("Work Order name is required", invokeValidateWorkOrder(dto, Constants.CREATE));
        dto.setWorkOrderName("name");
        assertNull(invokeValidateWorkOrder(dto, Constants.CREATE));
    }

    @Test
    void testValidateWorkAllocation_AllBranches() {
        WorkAllocationDTO dto = new WorkAllocationDTO();
        assertEquals("Work Order Id is required", invokeValidateWorkAllocation(dto, Constants.CREATE));
        dto.setWorkOrderId("wo1");
        assertEquals("Designation is required", invokeValidateWorkAllocation(dto, Constants.CREATE));
        dto.setDesignation("desig");
        assertNull(invokeValidateWorkAllocation(dto, Constants.CREATE));
    }

    @Test
    void testUpdateWorkOrder_EmptyMapInList() {
        String token = "token";
        String userId = "user1";
        WorkOrderDTO dto = new WorkOrderDTO();
        dto.setWorkOrderId("wo1");
        when(accessTokenValidator.fetchUserIdFromAccessToken(token)).thenReturn(userId);
        when(cassandraOperation.getRecordsByPropertiesByKey(any(), any(), any(), any(), any()))
                .thenReturn(List.of(new HashMap<>()));
        ApiResponse response = service.updateWorkOrder(token, dto);
        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
    }

    @Test
    void testReadWorkOrder_MapperThrowsException_TypeReference() throws Exception {
        String token = "token";
        String userId = "user1";
        String workOrderId = "wo1";
        Map<String, Object> record = new HashMap<>();
        record.put(Constants.DATA, "{}");
        List<Map<String, Object>> records = List.of(record);

        when(accessTokenValidator.fetchUserIdFromAccessToken(token)).thenReturn(userId);
        when(cassandraOperation.getRecordsByPropertiesByKey(any(), any(), any(), any(), any())).thenReturn(records);
        when(mapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenThrow(new JsonProcessingException("error") {});

        ApiResponse response = service.readWorkOrder(token, workOrderId);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
    }

    private String invokeValidateWorkOrder(WorkOrderDTO dto, String type) {
        try {
            Method m = WorkAllocationServiceImpl.class.getDeclaredMethod("validateWorkOrder", WorkOrderDTO.class, String.class);
            m.setAccessible(true);
            return (String) m.invoke(service, dto, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String invokeValidateWorkAllocation(WorkAllocationDTO dto, String type) {
        try {
            Method m = WorkAllocationServiceImpl.class.getDeclaredMethod("validateWorkAllocation", WorkAllocationDTO.class, String.class);
            m.setAccessible(true);
            return (String) m.invoke(service, dto, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testReadWorkOrder_Success_TypeReference() throws Exception {
        String token = "token";
        String userId = "user1";
        String workOrderId = "wo1";
        Map<String, Object> record = new HashMap<>();
        record.put(Constants.DATA, "{}");
        List<Map<String, Object>> records = List.of(record);
        when(accessTokenValidator.fetchUserIdFromAccessToken(token)).thenReturn(userId);
        when(cassandraOperation.getRecordsByPropertiesByKey(any(), any(), any(), any(), any())).thenReturn(records);
        when(mapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(new HashMap<>());
        ApiResponse response = service.readWorkOrder(token, workOrderId);
        assertEquals(HttpStatus.OK, response.getResponseCode());
    }

    @Test
    void testUpdateWorkAllocationFields_AllFieldsSet()  throws  Exception{
        WorkAllocationDTO source = new WorkAllocationDTO();
        source.setWorkOrderId("wo1");
        source.setDesignation("desig");
        source.setUserId("user1");
        source.setUserName("uname");
        source.setDescription("desc");
        source.setWorkAllocationStatus("status");// Correct usage
        source.setRoleActivitiesDetails(List.of(new RolesActivitiesDTO(), new RolesActivitiesDTO()));
        source.setCompetenciesV6(List.of(new HashMap<String, Object>(), new HashMap<String, Object>()));
        WorkAllocationDTO target = new WorkAllocationDTO();
        Method m = WorkAllocationServiceImpl.class.getDeclaredMethod(
                "updateWorkAllocationFields", WorkAllocationDTO.class, WorkAllocationDTO.class);
        m.setAccessible(true);
        m.invoke(null, source, target);
        assertEquals("wo1", target.getWorkOrderId());
        assertEquals("desig", target.getDesignation());
        assertEquals("user1", target.getUserId());
        assertEquals("uname", target.getUserName());
        assertEquals("desc", target.getDescription());
        assertEquals("status", target.getWorkAllocationStatus());
        assertEquals(2, target.getRoleActivitiesDetails().size());
        assertEquals(2, target.getCompetenciesV6().size());
    }

    @Test
    void testUpdateWorkOrderFields_AllFieldsSet() throws Exception {
        WorkOrderDTO source = new WorkOrderDTO();
        source.setWorkOrderName("Order1");
        source.setWorkOrderStatus("Active");
        source.setPublishedOn("2024-06-01");
        source.setPublishedBy("admin");
        source.setGeneratedWorkOrderPdfLink("link1");
        source.setSignedPdfLink("link2");
        source.setUserIds(List.of("u1", "u2"));
        source.setWorkAllocationIds(List.of("wa1", "wa2"));
        WorkOrderDTO target = new WorkOrderDTO();
        Method m = WorkAllocationServiceImpl.class.getDeclaredMethod(
                "updateWorkOrderFields", WorkOrderDTO.class, WorkOrderDTO.class, String.class);
        m.setAccessible(true);
        m.invoke(new WorkAllocationServiceImpl(), source, target, "userX");
        assertEquals("Order1", target.getWorkOrderName());
        assertEquals("Active", target.getWorkOrderStatus());
        assertEquals("2024-06-01", target.getPublishedOn());
        assertEquals("admin", target.getPublishedBy());
        assertEquals("link1", target.getGeneratedWorkOrderPdfLink());
        assertEquals("link2", target.getSignedPdfLink());
        assertEquals(List.of("u1", "u2"), target.getUserIds());
        assertEquals(List.of("wa1", "wa2"), target.getWorkAllocationIds());
        assertEquals("userX", target.getUpdatedBy());
        assertNotNull(target.getUpdatedAt());
    }

    @Test
    void testUpdateWorkOrderFields_PartialUpdate() throws Exception {
        WorkOrderDTO source = new WorkOrderDTO();
        source.setWorkOrderName("Order2");
        WorkOrderDTO target = new WorkOrderDTO();
        target.setWorkOrderStatus("OldStatus");
        target.setUserIds(List.of("oldUser"));
        Method m = WorkAllocationServiceImpl.class.getDeclaredMethod(
                "updateWorkOrderFields", WorkOrderDTO.class, WorkOrderDTO.class, String.class);
        m.setAccessible(true);
        m.invoke(new WorkAllocationServiceImpl(), source, target, "userY");
        assertEquals("Order2", target.getWorkOrderName());
        assertEquals("OldStatus", target.getWorkOrderStatus()); // Not overwritten
        assertEquals(List.of("oldUser"), target.getUserIds()); // Not overwritten
        assertEquals("userY", target.getUpdatedBy());
        assertNotNull(target.getUpdatedAt());
    }

    @Test
    void testUpdateWorkOrderFields_EmptySourceDoesNotUpdate() throws Exception {
        WorkOrderDTO source = new WorkOrderDTO(); // All fields empty
        WorkOrderDTO target = new WorkOrderDTO();
        target.setWorkOrderName("Existing");
        target.setUpdatedBy("oldUser");
        target.setUpdatedAt("oldTime");
        Method m = WorkAllocationServiceImpl.class.getDeclaredMethod(
                "updateWorkOrderFields", WorkOrderDTO.class, WorkOrderDTO.class, String.class);
        m.setAccessible(true);
        m.invoke(new WorkAllocationServiceImpl(), source, target, "userZ");
        assertEquals("Existing", target.getWorkOrderName()); // Not overwritten
        assertEquals("userZ", target.getUpdatedBy());
        assertNotEquals("oldTime", target.getUpdatedAt());
    }
}
