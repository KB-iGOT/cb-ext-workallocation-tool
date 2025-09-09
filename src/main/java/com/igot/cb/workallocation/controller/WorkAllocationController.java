package com.igot.cb.workallocation.controller;

import com.igot.cb.workallocation.model.WorkAllocationDTO;
import com.igot.cb.workallocation.model.WorkOrderDTO;
import com.igot.cb.workallocation.service.IWorkAllocationService;
import com.igot.cb.workallocation.storage.service.StorageService;
import com.igot.cb.workallocation.util.ApiResponse;
import com.igot.cb.workallocation.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/v1")
public class WorkAllocationController {

    @Autowired
    private IWorkAllocationService workAllocationService;

    @Autowired
    private StorageService storageService;

    @PostMapping("/workallocation/create")
    public ResponseEntity<ApiResponse> createWorkAllocation(@RequestHeader(Constants.X_AUTH_TOKEN) String authUserToken, @RequestBody WorkAllocationDTO workAllocation) {
        return new ResponseEntity<>(workAllocationService.createWorkAllocation(authUserToken, workAllocation),
                HttpStatus.OK);
    }

    @PostMapping("/workorder/create")
    public ResponseEntity<ApiResponse> createWorkOrder(@RequestHeader(Constants.X_AUTH_TOKEN) String authUserToken, @RequestBody WorkOrderDTO workOrderDTO) {
        return new ResponseEntity<>(workAllocationService.createWorkOrder(authUserToken, workOrderDTO),
                HttpStatus.OK);
    }

    @PostMapping("/workallocation/update")
    public ResponseEntity<ApiResponse> updateWorkAllocation(@RequestHeader(Constants.X_AUTH_TOKEN) String authUserToken, @RequestBody WorkAllocationDTO workAllocation) {
        return new ResponseEntity<>(workAllocationService.updateWorkAllocation(authUserToken, workAllocation),
                HttpStatus.OK);
    }

    @PostMapping("/workorder/update")
    public ResponseEntity<ApiResponse> updateWorkOrder(@RequestHeader(Constants.X_AUTH_TOKEN) String authUserToken, @RequestBody WorkOrderDTO workAllocation) {
        return new ResponseEntity<>(workAllocationService.updateWorkOrder(authUserToken, workAllocation),
                HttpStatus.OK);
    }

    @PostMapping("/upload/pdf/workorder")
    public ResponseEntity<ApiResponse> uploadFileToGCPContainer(@Valid @RequestBody Map<String, Object> requestBody, @RequestHeader("x-authenticated-user-token") String authUserToken) {
        ApiResponse uploadResponse = storageService.uploadFileToGCPContainer(requestBody, authUserToken);
        return new ResponseEntity<>(uploadResponse, uploadResponse.getResponseCode());
    }

    @GetMapping("/workallocation/read/{workOrderId}/{userId}")
    public ResponseEntity<ApiResponse> readWorkAllocation(
            @RequestHeader(Constants.X_AUTH_TOKEN) String authUserToken,
            @PathVariable(Constants.WORK_ORDER_ID_KEY) String workOrderId,
            @PathVariable(Constants.USER_ID) String userId) {
        ApiResponse response = workAllocationService.readWorkAllocation(authUserToken, workOrderId, userId);
        return ResponseEntity.status(response.getResponseCode()).body(response);
    }

    @GetMapping("/workorder/read/{workOrderId}")
    public ResponseEntity<ApiResponse> readWorkOrder(
            @RequestHeader(Constants.X_AUTH_TOKEN) String authUserToken,
            @PathVariable(Constants.WORK_ORDER_ID_KEY) String workOrderId) {
        ApiResponse response = workAllocationService.readWorkOrder(authUserToken, workOrderId);
        return ResponseEntity.status(response.getResponseCode()).body(response);
    }

}
