package org.cloudcompare.backend.catalog;

import java.util.List;

public class Compare {
    private final String serviceId;
    private final String unit;
    private final String currency;
    private final String region;


    private final ServiceOffer selectedOffer;
    private final Price selectedPrice;

    private final List<ServiceOffer> comparedOffers;
    private final List<Price> comparedPrices;

    public Compare(
            ServiceOffer selectedOffer,
            Price selectedPrice,
            String serviceId,
            String region,
            String unit,
            String currency,
            List<ServiceOffer> comparedOffers,
            List<Price> comparedPrices
    ) {
        this.selectedOffer = selectedOffer;
        this.selectedPrice = selectedPrice;
        this.serviceId = serviceId;
        this.region = region;
        this.unit = unit;
        this.currency = currency;
        this.comparedOffers = comparedOffers;
        this.comparedPrices = comparedPrices;
    }

    public List<Price> getComparedPrices() {return comparedPrices;}
    public List<ServiceOffer> getComparedOffers() {return comparedOffers;}
    public ServiceOffer getSelectedOffer() { return selectedOffer; }
    public Price getSelectedPrice() { return selectedPrice; }

    public String getServiceId() { return serviceId; }
    public String getUnit() { return unit; }
    public String getCurrency() { return currency; }
    public String getRegion() { return region; }

}
