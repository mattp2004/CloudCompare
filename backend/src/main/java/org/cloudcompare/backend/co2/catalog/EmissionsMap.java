package org.cloudcompare.backend.co2.catalog;

public class EmissionsMap {
    public final String provider;
    public final String region;
    public final String serviceId;

    public long offerCount;
    public final String rating; //A-e

    public final double avgCo2KgPerHour;
    public final double avgCo2KgPerMonth;


    public EmissionsMap(String provider, String region, String serviceId, long offerCount, String rating, double avgCo2KgPerHour) {
        this.provider = provider;
        this.region = region;
        this.serviceId = serviceId;
        this.offerCount = offerCount;
        this.rating = rating;
        this.avgCo2KgPerHour = round_co2(avgCo2KgPerHour);
        this.avgCo2KgPerMonth = round_co2(avgCo2KgPerHour*((double) (365 * 24) /12));
    }

    public static double round_co2(double d){
        return Math.round(d*1000.0)/1000.0;
    }
}
