package de.seuhd.campuscoffee.domain.impl;

import de.seuhd.campuscoffee.domain.exceptions.DuplicatePosNameException;
import de.seuhd.campuscoffee.domain.exceptions.OsmNodeMissingFieldsException;
import de.seuhd.campuscoffee.domain.exceptions.OsmNodeNotFoundException;
import de.seuhd.campuscoffee.domain.model.CampusType;
import de.seuhd.campuscoffee.domain.model.OsmNode;
import de.seuhd.campuscoffee.domain.model.Pos;
import de.seuhd.campuscoffee.domain.exceptions.PosNotFoundException;
import de.seuhd.campuscoffee.domain.model.PosType;
import de.seuhd.campuscoffee.domain.ports.OsmDataService;
import de.seuhd.campuscoffee.domain.ports.PosDataService;
import de.seuhd.campuscoffee.domain.ports.PosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of the POS service that handles business logic related to POS entities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PosServiceImpl implements PosService {
    private final PosDataService posDataService;
    private final OsmDataService osmDataService;

    @Override
    public void clear() {
        log.warn("Clearing all POS data");
        posDataService.clear();
    }

    @Override
    public @NonNull List<Pos> getAll() {
        log.debug("Retrieving all POS");
        return posDataService.getAll();
    }

    @Override
    public @NonNull Pos getById(@NonNull Long id) throws PosNotFoundException {
        log.debug("Retrieving POS with ID: {}", id);
        return posDataService.getById(id);
    }

    @Override
    public @NonNull Pos upsert(@NonNull Pos pos) throws PosNotFoundException {
        if (pos.id() == null) {
            // Create new POS
            log.info("Creating new POS: {}", pos.name());
            return performUpsert(pos);
        } else {
            // Update existing POS
            log.info("Updating POS with ID: {}", pos.id());
            // POS ID must be set
            Objects.requireNonNull(pos.id());
            // POS must exist in the database before the update
            posDataService.getById(pos.id());
            return performUpsert(pos);
        }
    }

    @Override
    public @NonNull Pos importFromOsmNode(@NonNull Long nodeId) throws OsmNodeNotFoundException {
        log.info("Importing POS from OpenStreetMap node {}...", nodeId);

        // Fetch the OSM node data using the port
        OsmNode osmNode = osmDataService.fetchNode(nodeId);

        // Convert OSM node to POS domain object and upsert it
        // TODO: Implement the actual conversion (the response is currently hard-coded).
        Pos savedPos = upsert(convertOsmNodeToPos(osmNode));
        log.info("Successfully imported POS '{}' from OSM node {}", savedPos.name(), nodeId);

        return savedPos;
    }

    /**
     * Converts an OSM node to a POS domain object.
     * Extracts relevant tags from OpenStreetMap data and maps them to POS fields.
     */
    private @NonNull Pos convertOsmNodeToPos(@NonNull OsmNode osmNode) {
        Map<String, String> tags = osmNode.tags();
        
        String name = extractName(tags, osmNode.nodeId());
        String description = extractDescription(tags);
        PosType type = extractPosType(tags, osmNode.nodeId());
        String street = extractRequiredTag(tags, "addr:street", osmNode.nodeId());
        String houseNumber = extractRequiredTag(tags, "addr:housenumber", osmNode.nodeId());
        Integer postalCode = extractPostalCode(tags, osmNode.nodeId());
        String city = extractRequiredTag(tags, "addr:city", osmNode.nodeId());
        
        return Pos.builder()
                .name(name)
                .description(description)
                .type(type)
                .campus(CampusType.ALTSTADT)
                .street(street)
                .houseNumber(houseNumber)
                .postalCode(postalCode)
                .city(city)
                .build();
    }
    
    private @NonNull String extractName(@NonNull Map<String, String> tags, @NonNull Long nodeId) {
        if (tags.containsKey("name:de")) {
            return tags.get("name:de");
        } else if (tags.containsKey("name:en")) {
            return tags.get("name:en");
        } else if (tags.containsKey("name")) {
            return tags.get("name");
        }
        log.error("Required tag 'name' missing for OSM node {}", nodeId);
        throw new OsmNodeMissingFieldsException(nodeId);
    }
    
    private @NonNull String extractDescription(@NonNull Map<String, String> tags) {
        if (tags.containsKey("description")) {
            return tags.get("description");
        }
        return "Imported from OpenStreetMap";
    }
    
    private @NonNull PosType extractPosType(@NonNull Map<String, String> tags, @NonNull Long nodeId) {
        String amenity = tags.get("amenity");
        String shop = tags.get("shop");
        
        if ("cafe".equals(amenity) || "biergarten".equals(amenity)) {
            return PosType.CAFE;
        } else if ("bakery".equals(amenity) || "bakery".equals(shop)) {
            return PosType.BAKERY;
        } else if ("vending_machine".equals(amenity)) {
            return PosType.VENDING_MACHINE;
        } else if ("restaurant".equals(amenity) || "fast_food".equals(amenity)) {
            return PosType.CAFETERIA;
        }
        
        log.warn("Unknown amenity type '{}' for OSM node {}, defaulting to CAFE", amenity, nodeId);
        return PosType.CAFE;
    }
    
    private @NonNull String extractRequiredTag(@NonNull Map<String, String> tags, @NonNull String key, @NonNull Long nodeId) {
        if (!tags.containsKey(key) || tags.get(key).isBlank()) {
            log.error("Required tag '{}' missing for OSM node {}", key, nodeId);
            throw new OsmNodeMissingFieldsException(nodeId);
        }
        return tags.get(key);
    }
    
    private @NonNull Integer extractPostalCode(@NonNull Map<String, String> tags, @NonNull Long nodeId) {
        String postalCodeStr = extractRequiredTag(tags, "addr:postcode", nodeId);
        try {
            return Integer.parseInt(postalCodeStr);
        } catch (NumberFormatException e) {
            log.error("Invalid postal code '{}' for OSM node {}", postalCodeStr, nodeId);
            throw new OsmNodeMissingFieldsException(nodeId);
        }
    }

    /**
     * Performs the actual upsert operation with consistent error handling and logging.
     * Database constraint enforces name uniqueness - data layer will throw DuplicatePosNameException if violated.
     * JPA lifecycle callbacks (@PrePersist/@PreUpdate) set timestamps automatically.
     *
     * @param pos the POS to upsert
     * @return the persisted POS with updated ID and timestamps
     * @throws DuplicatePosNameException if a POS with the same name already exists
     */
    private @NonNull Pos performUpsert(@NonNull Pos pos) throws DuplicatePosNameException {
        try {
            Pos upsertedPos = posDataService.upsert(pos);
            log.info("Successfully upserted POS with ID: {}", upsertedPos.id());
            return upsertedPos;
        } catch (DuplicatePosNameException e) {
            log.error("Error upserting POS '{}': {}", pos.name(), e.getMessage());
            throw e;
        }
    }
}
