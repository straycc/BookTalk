package com.cc.booktalk.infrastructure.config;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class ElasticsearchConfig {

    @Bean
    public RestClient restClient(@Value("${spring.elasticsearch.uris}") String uri) {
        String firstUri = uri.split(",")[0].trim();
        return RestClient.builder(HttpHost.create(firstUri)).build();
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(RestClient restClient, ObjectMapper objectMapper) {
        JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper(objectMapper);
        RestClientTransport transport = new RestClientTransport(restClient, jsonpMapper);
        return new ElasticsearchClient(transport);
    }
}

