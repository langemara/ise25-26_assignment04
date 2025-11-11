package de.seuhd.campuscoffee.domain.model;

import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Represents an OpenStreetMap node with relevant Point of Sale information.
 * This is the domain model for OSM data before it is converted to a POS object.
 *
 * @param nodeId The OpenStreetMap node ID.
 * @param latitude The latitude coordinate.
 * @param longitude The longitude coordinate.
 * @param tags Map of OSM tags (key-value pairs).
 */
@Builder
public record OsmNode(
        @NonNull Long nodeId,
        @Nullable Double latitude,
        @Nullable Double longitude,
        @NonNull Map<String, String> tags
) {
}
