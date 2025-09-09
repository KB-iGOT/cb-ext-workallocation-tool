package com.igot.cb.workallocation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RolesActivitiesDTO {
    private String type;
    private String description;
    private List<String> activities;
    private String submissionUserId;
    private String submissionUserName;
}
