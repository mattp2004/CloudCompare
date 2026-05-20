package org.cloudcompare.backend.co2;

import org.cloudcompare.backend.catalog.OfferType;
import org.cloudcompare.backend.catalog.ServiceOffer;
import org.cloudcompare.backend.co2.catalog.Emissions;

import java.util.Map;

import static java.util.Map.entry;

public class EmissionEngine {
    //Carbon Emission-Region-Based factors
    private static final Map<String,Double> EmissionFactors = Map.ofEntries(
        entry("US-CENTRAL", 450.0),
        entry("US-EAST", 386.0),
        entry("US-WEST", 248.0),

        entry("CANADA", 110.0),

        entry("UK", 178.0),

        entry("EU-NORTH",46.0),
        entry("EU-CENTRAL", 340.0),
        entry("EU-WEST", 218.0),
        entry("EU-SOUTH", 280.0),

        entry("SOUTH_AMERICA", 190.0),

        entry("ASIA-NORTHEAST", 423.0),
        entry("ASIA-SOUTH", 708.0),
        entry("ASIA-SOUTHEAST", 600.0),
        entry("ASIA-EAST", 555.0),

        entry("AUSTRALIA",490.0),

        entry("MIDDLE_EAST", 540.0),

        entry("AFRICA", 598.0)
    );

    private static final Map<String,Double> ProviderPUE = Map.ofEntries(
            entry("AWS", 1.15),
            entry("GCP", 1.10),
            entry("AZURE", 1.18)
    );

    private static final Map<OfferType, Double> typeWattage = Map.ofEntries(
            entry(OfferType.ON_DEMAND, 150.0),
            entry(OfferType.RESERVED, 150.0),
            entry(OfferType.SPOT, 150.0),
            entry(OfferType.DATA_TRANSFER, 5.0),
            entry(OfferType.API_REQUEST, 2.0),
            entry(OfferType.UNKNOWN, 50.0)
    );

    public static Emissions calculateEmissions(ServiceOffer offer){
        String region = offer.getRegion().toUpperCase();
        String provider = offer.getProvider().toUpperCase();

        double emissionFactor = EmissionFactors.getOrDefault(region,445.0);
        double providerPue = ProviderPUE.getOrDefault(provider, 1.20);
        double watts = typeWattage.getOrDefault(offer.getOfferType(), 50.0);

        double co2KgPerHour = watts * providerPue * (emissionFactor/ 1_000_000.0);
        return new Emissions(offer.getId(), offer.getProvider(), offer.getRegion(),co2KgPerHour);
    }

    public static String rate(double co2ph){
        if(co2ph<0.010) return "A";
        if(co2ph<0.050) return "B";
        if(co2ph<0.100) return "C";
        if(co2ph<0.200) return "D";
        return ("E");
    }
}
