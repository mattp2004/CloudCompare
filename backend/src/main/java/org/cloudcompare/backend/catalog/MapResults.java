package org.cloudcompare.backend.catalog;

public class MapResults {
    public final String region;
    public final String provider;     // optional grouping (can be null if you decide)
    public final String serviceId;    // optional grouping (can be null if you decide)

    public final long offerCount;

    public final String currency;     // might be null if no prices
    public final String unit;         // might be null if no prices
    public final Double minPrice;     // null if no prices
    public final Double avgPrice;     // null if no prices

    public MapResults(
            String region,
            String provider,
            String serviceId,
            long offerCount,
            String currency,
            String unit,
            Double minPrice,
            Double avgPrice
    ) {
        this.region = region;
        this.provider = provider;
        this.serviceId = serviceId;
        this.offerCount = offerCount;
        this.currency = currency;
        this.unit = unit;
        this.minPrice = minPrice;
        this.avgPrice = avgPrice;
    }
}