package com.igot.cb.workallocation.storage.service;

import com.igot.cb.workallocation.util.ApiResponse;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.Map;

public interface StorageService {

    /**
     * Uploads a file to a GCP container.
     *
     * @param requestBody   the request body containing file details
     * @param authUserToken the authentication token of the user
     * @return ApiResponse indicating the result of the upload operation
     */
    ApiResponse uploadFileToGCPContainer(@Valid Map<String, Object> requestBody, String authUserToken);
}
