package org.cloudcompare.backend.catalog;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Map;

@JsonPropertyOrder({
        "id",
        "serviceId",
        "provider",
        "sku",
        "name",
        "description",
        "offerType",
        "region",
        "specsJson"
})
public class ServiceOffer {

    private final String id;
    private final String serviceId;
    private final String provider;
    private final String sku;
    private final String name;
    private final String description;
    private final OfferType type;
    private final String region;

    private final Map<String, Object> specsJson;

    public ServiceOffer(
            String id,
            String serviceId,
            String provider,
            String sku,
            String name,
            String description,
            OfferType type,
            String region,
            Map<String, Object> specsJson
    ) {
        this.id = id;
        this.serviceId = serviceId;
        this.provider = provider;
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.type = type;
        this.region = region;
        this.specsJson = specsJson;
    }

    public String getId() { return id; }
    public String getServiceId() { return serviceId; }
    public String getProvider() { return provider; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public OfferType getOfferType() {return type;}
    public String getRegion() { return region; }
    public Map<String, Object> getSpecsJson() { return specsJson; }
}
