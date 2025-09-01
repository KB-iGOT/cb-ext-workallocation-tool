package com.igot.cb.workallocation.transactional.elasticsearch.service;

import java.util.Map;

public interface EsUtilService {

    String addDocument(String esIndexName, String id, Map<String, Object> document, String jsonFilePath);
}
