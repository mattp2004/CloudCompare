package org.cloudcompare.backend.catalog;

import java.math.BigDecimal;
import java.util.Map;

public class DataTransfer {
    public String serviceId;

    public String provider;
    public String type;

    public String fromLocation;
    public String toLocation;

    public String fromRegion;
    public String toRegion;

    public Map<String, Object> specsJson;

    public BigDecimal price;
    public String unit;

}
