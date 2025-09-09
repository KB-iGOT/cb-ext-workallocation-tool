package com.igot.cb.workallocation.transactional.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.workallocation.util.CbServerProperties;
import com.networknt.schema.JsonSchemaFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;


@Service
@Slf4j
public class EsUtilServiceImpl implements EsUtilService {

    private final ElasticsearchClient elasticsearchClient;

    @Autowired
    CbServerProperties cbProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    public EsUtilServiceImpl(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }


    @Override
    public String addDocument(
            String esIndexName, String id, Map<String, Object> document, String jsonFilePath) {
        try {
            JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance();
            InputStream schemaStream = schemaFactory.getClass().getResourceAsStream(jsonFilePath);
            Map<String, Object> map = objectMapper.readValue(schemaStream,
                    new TypeReference<Map<String, Object>>() {
                    });
            Iterator<Map.Entry<String, Object>> iterator = document.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Object> entry = iterator.next();
                String key = entry.getKey();
                if (!map.containsKey(key)) {
                    iterator.remove();
                }
            }
            IndexRequest<Map<String, Object>> indexRequest = new IndexRequest.Builder<Map<String, Object>>()
                    .index(esIndexName)
                    .id(id)
                    .document(document)
                    .refresh(Refresh.True)
                    .build();
            IndexResponse response = elasticsearchClient.index(indexRequest);
            return "Successfully indexed document with id: " + response.result();
        } catch (Exception e) {
            log.error("Issue while Indexing to es: {}", e.getMessage(), e);
            return null;
        }
    }
}
