package org.cloudcompare.backend.ingest.util;

import org.cloudcompare.backend.catalog.Provider;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;

public final class UnitNormaliser {

    private UnitNormaliser() {}

    public static final class NormalizedUnit {
        public final String unit;
        public final BigDecimal price;
        public NormalizedUnit(String unit, BigDecimal price) {
            this.unit = unit;
            this.price = price;
        }
    }

    private static final BigDecimal GB_TO_GIB = new BigDecimal("1.073741");
    private static final BigDecimal MB_PER_GIB = new BigDecimal("1073.741");

    private final static Map<String, String> UNIT_MAPPER = Map.ofEntries(
            Map.entry("HOUR", "h"),
            Map.entry("HRS", "h"),
            Map.entry("1HOUR", "h"),
            Map.entry("1_HOUR", "h"),
            Map.entry("HR", "h"),
            Map.entry("H", "h"),

            Map.entry("1D", "day"),
            Map.entry("DAY", "day"),
            Map.entry("1DAY", "day"),
            Map.entry("ONE_DAY", "day"),
            Map.entry("D", "day"),

            Map.entry("MONTH", "mo"),
            Map.entry("1MO", "mo"),
            Map.entry("1MON", "mo"),
            Map.entry("1MONTH", "mo"),
            Map.entry("MO", "mo"),
            Map.entry("MIN", "min"),
            Map.entry("1MIN", "min"),
            Map.entry("MINUTE", "min"),
            Map.entry("1MINUTE", "min"),
            Map.entry("1MIN", "min"),


            Map.entry("GIBY", "GiB"),
            Map.entry("GIB", "GiB"),
            Map.entry("1GIB", "GiB"),
            Map.entry("1GIBY", "GiB"),
            Map.entry("ONE_GIB", "GiB"),

            Map.entry("GIBYH", "GiB-h"),
            Map.entry("1GIBHOUR", "GiB-h"),
            Map.entry("GIBY-PH", "GiB-h"),
            Map.entry("GIBHOUR", "GiB-h"),

            Map.entry("GIBYD", "GiB-day"),
            Map.entry("1GBDAY", "GiB-day"),
            Map.entry("1GIBDAY", "GiB-day"),
            Map.entry("GIBDAY", "GiB-day"),
            Map.entry("ONEGBDAY", "GiB-day"),

            Map.entry("GIBYMO", "GiB-Mo"),
            Map.entry("1GIBMONTH", "GiB-Mo"),
            Map.entry("GIBMONTH", "GiB-Mo"),
            Map.entry("ONE_GB_MO", "GiB-Mo"),

            Map.entry("COUNT", "count"),
            Map.entry("1", "count"),
            Map.entry("APICALLS", "count"),
            Map.entry("BUCKETMO", "count-Mo")
    );

    public static NormalizedUnit normalise(Provider provider, String rawUnit, BigDecimal rawPrice) {
        if (rawPrice == null) rawPrice = BigDecimal.ZERO;
        if (rawUnit == null) rawUnit = "1";

        String formatUnit = formatUnit(rawUnit);
        if(rawUnit.contains("/")){
            if(formatUnit.equals("1HOUR")){
                return new NormalizedUnit("count-h", rawPrice);
            }
            if(formatUnit.equals("1MONTH")){
                return new NormalizedUnit("count-h", rawPrice);
            }
            if(formatUnit.equals("1MINUTE")){
                return new NormalizedUnit("count-h", rawPrice);
            }
        }

        if(formatUnit.equals("GBMO")||formatUnit.equals("GBMONTH")||formatUnit.equals("1GBMONTH")){
            return new NormalizedUnit("GiB", multiply(rawPrice, GB_TO_GIB));
        }

        if(formatUnit.equals("GBYH")||formatUnit.equals("GBYHOUR")){
            return new NormalizedUnit("Gib-h", multiply(rawPrice, GB_TO_GIB));
        }

        if(formatUnit.equals("GB")||formatUnit.equals("1GB")||formatUnit.equals("GBY")||formatUnit.equals("1GB")||formatUnit.equals("1GBY")){
            return new NormalizedUnit("GiB", multiply(rawPrice, GB_TO_GIB));
        }

        if(formatUnit.equals("MB")||formatUnit.equals("1MB")){
            return new NormalizedUnit("GiB", multiply(rawPrice, MB_PER_GIB));
        }

        if(formatUnit.equals("10KMONTH")){
            return new NormalizedUnit("count-Mo", divide(rawPrice,10_000));
        }
        if(formatUnit.equals("10K")){
            return new NormalizedUnit("count", divide(rawPrice,10_000));
        }
        if(formatUnit.equals("1M")){
            return new NormalizedUnit("count", divide(rawPrice,1_000_000));
        }
        if(formatUnit.equals("1MMONTH")){
            return new NormalizedUnit("count-Mo", divide(rawPrice,1_000_000));
        }

        if(formatUnit.equals("100")){
            return new NormalizedUnit("count", divide(rawPrice,100));
        }
        if(formatUnit.equals("100M")){
            return new NormalizedUnit("count-Mo", divide(rawPrice,100));
        }
        if(formatUnit.equals("100MONTH")){
            return new NormalizedUnit("count-Mo", divide(rawPrice,100));
        }

        //rest map

        String mapped = UNIT_MAPPER.get(formatUnit);
        if(mapped != null){
            return new NormalizedUnit(mapped, rawPrice);
        }
        return new NormalizedUnit(rawUnit, rawPrice);
    }



    private static String formatUnit(String in){
        String format = "";
        format = in.trim();
        if(format.isEmpty()){
            format = "1";
        }

        format =format.toUpperCase(Locale.ROOT).replace(".","").replace("/","").replace("-","").replace("_","").replace(" ", "");
        return format;
    }

    private static BigDecimal divide(BigDecimal value, int by) {
        if (by < 0) return value;
        if(by ==0) return value;
        return value.divide(new BigDecimal(by), 12, RoundingMode.HALF_UP);
    }

    private static BigDecimal multiply(BigDecimal value, BigDecimal by) {
        if (value == null) return BigDecimal.ZERO;
        return value.multiply(by).setScale(12, RoundingMode.HALF_UP);
    }
}
