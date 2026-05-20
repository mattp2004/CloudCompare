package org.cloudcompare.backend.co2.catalog;

import org.cloudcompare.backend.co2.EmissionEngine;

public class Emissions {
    public final String offerId;
    public final String provider;
    public final String region;

    public final String rating; //A-e

    public final double co2KgPerHour;
    public final double co2KgPerMonth;


    public Emissions(String offerId, String provider, String region, double co2KgPerHour) {
        this.offerId = offerId;
        this.provider = provider;
        this.region = region;
        this.rating = EmissionEngine.rate(co2KgPerHour);
        this.co2KgPerHour = round_co2(co2KgPerHour);
        this.co2KgPerMonth = round_co2(co2KgPerHour*((double) (365 * 24) /12));
    }

    public static double round_co2(double d){
        return Math.round(d*1000.0)/1000.0;
    }
}
