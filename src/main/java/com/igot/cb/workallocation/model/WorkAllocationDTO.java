package com.igot.cb.workallocation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkAllocationDTO {
    private String workAllocationId;
    private String designation;
    private String userId;
    private String userName;
    private String description;
    private String workOrderId;
    private List<RolesActivitiesDTO> roleActivitiesDetails;
    private List<Map<String, Object>> competenciesV6;
    private String workAllocationStatus;
}
