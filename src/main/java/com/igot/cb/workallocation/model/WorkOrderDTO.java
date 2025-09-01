package com.igot.cb.workallocation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkOrderDTO {
    private String workOrderId;
    private String createdBy;
    private String updatedBy;
    private String createdAt;
    private String updatedAt;
    private String workOrderName;
    private String publishedOn;
    private String publishedBy;
    private String workOrderStatus;
    private String generatedWorkOrderPdfLink;
    private String signedPdfLink;
    private List<String> userIds;
    private List<String> workAllocationIds;

}
