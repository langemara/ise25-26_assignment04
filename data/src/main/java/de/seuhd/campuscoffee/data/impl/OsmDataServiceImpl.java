package de.seuhd.campuscoffee.data.impl;

import com.fasterxml.jackson.databind.JsonNode;
import de.seuhd.campuscoffee.domain.exceptions.OsmNodeNotFoundException;
import de.seuhd.campuscoffee.domain.model.OsmNode;
import de.seuhd.campuscoffee.domain.ports.OsmDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * OSM import service that fetches node data from OpenStreetMap API.
 */
@Service
@Slf4j
@RequiredArgsConstructor
class OsmDataServiceImpl implements OsmDataService {
    private static final String OSM_API_URL = "https://www.openstreetmap.org/api/0.6/node/{nodeId}.json";
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public @NonNull OsmNode fetchNode(@NonNull Long nodeId) throws OsmNodeNotFoundException {
        log.info("Fetching OSM node {} from OpenStreetMap API", nodeId);

        try {
            JsonNode response = restTemplate.getForObject(OSM_API_URL, JsonNode.class, nodeId);
            
            if (response == null || !response.has("elements") || response.get("elements").isEmpty()) {
                log.error("OSM node {} not found or response is empty", nodeId);
                throw new OsmNodeNotFoundException(nodeId);
            }

            JsonNode element = response.get("elements").get(0);
            
            Double latitude = element.has("lat") ? element.get("lat").asDouble() : null;
            Double longitude = element.has("lon") ? element.get("lon").asDouble() : null;
            
            Map<String, String> tags = new HashMap<>();
            if (element.has("tags")) {
                JsonNode tagsNode = element.get("tags");
                tagsNode.fields().forEachRemaining(entry -> 
                    tags.put(entry.getKey(), entry.getValue().asText())
                );
            }

            log.info("Successfully fetched OSM node {} with {} tags", nodeId, tags.size());
            
            return OsmNode.builder()
                    .nodeId(nodeId)
                    .latitude(latitude)
                    .longitude(longitude)
                    .tags(tags)
                    .build();
                    
        } catch (HttpClientErrorException.NotFound e) {
            log.error("OSM node {} not found: {}", nodeId, e.getMessage());
            throw new OsmNodeNotFoundException(nodeId);
        } catch (RestClientException e) {
            log.error("Failed to fetch OSM node {}: {}", nodeId, e.getMessage());
            throw new OsmNodeNotFoundException(nodeId);
        }
    }
}
