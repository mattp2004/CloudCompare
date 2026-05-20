package org.cloudcompare.backend.catalog;

import java.math.BigDecimal;

public class Price {
    private String offerID;
    private String currency;
    private BigDecimal price;
    private String unit;

    public Price(String _offerID, String _currency, BigDecimal _price, String _unit){
        this.offerID = _offerID;
        this.currency = _currency;
        this.price = _price;
        this.unit = _unit;
    }

    public String getOfferID() {return offerID;}
    public String getCurrency() {return currency;}
    public BigDecimal getPrice() {return price;}
    public String getUnit() {return unit;}
}
