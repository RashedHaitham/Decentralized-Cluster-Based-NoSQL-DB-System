package com.example.Database.cluster;

import com.example.Database.file.FileService;
import com.fasterxml.jackson.databind.JsonNode;
import org.json.simple.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class BroadcastService {

    // This internal method will do the broadcasting logic
    private static void broadcastInternal(String url, HttpMethod method, boolean isBroadcasted, Optional<JSONObject> dataOpt, Map<String, String> additionalHeaders) {
        try {
            JsonNode adminCredentials = FileService.readAdminCredentialsFromJson();
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("username", adminCredentials.get("username").asText());
            headers.set("password", adminCredentials.get("password").asText());
            headers.set("X-Broadcast", String.valueOf(isBroadcasted));
            additionalHeaders.forEach(headers::set);
            if (dataOpt.isPresent()) {
                headers.setContentType(MediaType.APPLICATION_JSON);
            }
            HttpEntity<String> requestEntity = dataOpt.map(jsonObject ->
                    new HttpEntity<>(jsonObject.toJSONString(), headers)).orElseGet(() -> new HttpEntity<>(headers));

            restTemplate.exchange(url, method, requestEntity, String.class);
        } catch (Exception e) {
            System.out.println("[ERROR] Broadcasting failed. URL: " + url);
            e.printStackTrace();
        }
    }

    public static class BroadcastRequestBuilder {
        private String url;
        private HttpMethod method;
        private boolean isBroadcasted;
        private Optional<JSONObject> dataOpt = Optional.empty();
        private Map<String, String> additionalHeaders = new HashMap<>();

        public BroadcastRequestBuilder withUrl(String url) {
            this.url = url;
            return this;
        }

        public BroadcastRequestBuilder withMethod(HttpMethod method) {
            this.method = method;
            return this;
        }

        public BroadcastRequestBuilder isBroadcasted(boolean isBroadcasted) {
            this.isBroadcasted = isBroadcasted;
            return this;
        }

        public BroadcastRequestBuilder withData(JSONObject data) {
            this.dataOpt = Optional.ofNullable(data);
            return this;
        }

        public BroadcastRequestBuilder withAdditionalHeaders(Map<String, String> additionalHeaders) {
            this.additionalHeaders = additionalHeaders;
            return this;
        }
        public void broadcast() {
            broadcastInternal(url, method, isBroadcasted, dataOpt, additionalHeaders);
        }
    }
}