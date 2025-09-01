package com.igot.cb.workallocation.controller;

import com.igot.cb.workallocation.model.WorkAllocationDTO;
import com.igot.cb.workallocation.model.WorkOrderDTO;
import com.igot.cb.workallocation.service.IWorkAllocationService;
import com.igot.cb.workallocation.util.ApiResponse;
import com.igot.cb.workallocation.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1")
public class WorkAllocationController {

    @Autowired
    private IWorkAllocationService workAllocationService;

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
}
