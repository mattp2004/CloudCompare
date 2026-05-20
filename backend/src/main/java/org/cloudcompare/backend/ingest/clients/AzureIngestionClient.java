package org.cloudcompare.backend.ingest.clients;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.cloudcompare.backend.catalog.OfferType;
import org.cloudcompare.backend.ingest.Ingestion;
import org.cloudcompare.backend.ingest.IngestionManager;
import org.cloudcompare.backend.ingest.util.RegionMapper;
import org.cloudcompare.backend.ingest.util.ServiceMapper;
import org.cloudcompare.backend.ingest.util.UnitNormaliser;
import org.cloudcompare.backend.util.Logger;
import org.cloudcompare.backend.catalog.Provider;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;

@Component
public class AzureIngestionClient {

    private static String BASE_URL ="https://prices.azure.com/api/retail/prices?$top=200";
    private static final int MAX_ITEMS = 900000;

    private final RestClient httpClient;
    private final DataSource dataSource;

    public Ingestion currentIngestion;
    public IngestionManager ingestionManager;


    AzureIngestionClient(RestClient.Builder httpBuilder, DataSource source, IngestionManager ingestionManager) {
        this.httpClient = httpBuilder.build();
        this.dataSource = source;
        this.ingestionManager = ingestionManager;
    }

    public Ingestion ingestAll() {
        currentIngestion = new Ingestion();
        currentIngestion.running = true;
        currentIngestion.provider = Provider.AZURE;
        ingestionManager.register(currentIngestion);

        String currentUrl = BASE_URL;

        boolean reachedLimit = false;
        while (currentUrl != null && !reachedLimit) {
            JsonObject rootObject = fetchAPI(currentUrl);
            if (rootObject == null || rootObject.isEmpty()) {
                Logger.LogError("Ingestion failed: Azure" + currentIngestion.startTime.toString());
                break;
            }

            JsonArray items = new JsonArray();
            if (rootObject.has("Items") &&  rootObject.get("Items").isJsonArray()) {
                items = rootObject.getAsJsonArray("Items");
            }

            for(int i = 0; i < items.size(); i ++){
                if(currentIngestion.offersStored>= MAX_ITEMS){
                    Logger.Log("Ingestiom limit reached; azure");
                    currentUrl= null;
                    reachedLimit=true;
                    break;
                }
                JsonObject o = items.get(i).getAsJsonObject();
                String rawServiceName ="";
                if(!o.has("serviceName")){
                    currentIngestion.recordError("No service name available");
                    continue;
                }
                else if(o.get("serviceName").isJsonNull()){
                    currentIngestion.recordError("No service name available");
                    continue;
                }

                try {
                    rawServiceName = o.get("serviceName").getAsString();
                } catch (Exception e) {
                    Logger.LogError("Failed to gather servicename");
                    currentIngestion.recordError("No service name available");
                    continue;
                }

                String formattedName = ServiceMapper.mapServiceName(rawServiceName, Provider.AZURE);

                if(formattedName==null){
                    currentIngestion.recordError("Failed to format service name");
                    continue;
                }
                insertOfferAndPrice(formattedName,o);
            }

            Logger.Log(currentUrl);
            if(rootObject.has("NextPageLink")&& !rootObject.get("NextPageLink").isJsonNull()){
                currentUrl = rootObject.get("NextPageLink").toString();
                currentUrl = currentUrl.replaceAll("\\$top=-\\d+", "\\$top=200");
                currentUrl = currentUrl.replace("&amp;","&");
            }
            else{
                currentUrl=null;
            }
        }

        currentIngestion.running = false;
        currentIngestion.finishTime = Instant.now();
        return currentIngestion;
    }

    private void insertOfferAndPrice(String properServiceName, JsonObject object) {

        String skuID = JsonToString(object, "skuId");
        if (skuID == null || skuID.isBlank()) return;

        String safeSKU = safe(skuID);

        String rawRegion = JsonToString(object, "armRegionName");
        String rawType;
        if(object.has("type")){
            rawType = JsonToString(object,"type");
        }else{
            rawType = "unknown";
        }
        String formattedRegion = RegionMapper.mapRegionName(rawRegion, Provider.AZURE);
        OfferType formattedType = formatOfferType(object);

        currentIngestion.incrementOfferType(formattedType);

        String safeType = safe(rawType).toString().toLowerCase();

        String offerId = "offer-az-" + formattedRegion + "-" + safeType + "-" + safeSKU;

        String provider = "AZURE";
        String sku = skuID;

        String name = firstNonBlank(JsonToString(object, "armSkuName"), JsonToString(object, "skuName"),skuID);
        String description = firstNonBlank(JsonToString(object, "meterName"), JsonToString(object, "productName"), JsonToString(object, "serviceName"), "N/A");
        String currency = "USD";
        currency =JsonToString(object, "currencyCode");
        BigDecimal price = getBigDecimal(object, "unitPrice");

        if (price == null){
            price = BigDecimal.ZERO;
        }

        String unit = "1";
        if(!object.get("unitOfMeasure").isJsonNull()){
            unit = JsonToString(object, "unitOfMeasure");
        }

        JsonObject specs = new JsonObject();
        specs.addProperty("raw_region_code", rawRegion);
        specs.addProperty("raw_location", firstNonBlank(JsonToString(object, "location"), ""));
        specs.addProperty("service_family", firstNonBlank(JsonToString(object, "serviceFamily"), ""));
        specs.addProperty("raw_type", firstNonBlank(JsonToString(object, "type"), ""));
        specs.addProperty("meter_id", firstNonBlank(JsonToString(object, "meterId"), ""));
        specs.addProperty("reservationTerm", firstNonBlank(JsonToString(object, "reservationTerm"), ""));
        specs.addProperty("raw_unit", unit);
        specs.addProperty("raw_price", price.toPlainString());

        String specsJson = specs.toString();


        UnitNormaliser.NormalizedUnit nu = UnitNormaliser.normalise(Provider.AZURE, unit,price);

        if (nu.price.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        String upsertOfferSql = """
                INSERT INTO service_offers
                (id, service_id, provider, sku, name, description, offer_type, region, specs)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?,?::jsonb)
                ON CONFLICT (id) DO UPDATE SET
                    service_id = EXCLUDED.service_id,
                    provider = EXCLUDED.provider,
                    sku = EXCLUDED.sku,
                    name = EXCLUDED.name,
                    description = EXCLUDED.description,
                    offer_type = EXCLUDED.offer_type,
                    region = EXCLUDED.region,
                    specs = EXCLUDED.specs
                """;

        String upsertPriceSql = """
                INSERT INTO prices (offer_id, currency, price, unit)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (offer_id) DO UPDATE SET
                    currency = EXCLUDED.currency,
                    price = EXCLUDED.price,
                    unit = EXCLUDED.unit
                """;

        try (Connection conn = dataSource.getConnection()) {

            try (PreparedStatement ps = conn.prepareStatement(upsertOfferSql)) {

                ps.setString(1, offerId);
                ps.setString(2, properServiceName);
                ps.setString(3, provider);
                ps.setString(4, sku);
                ps.setString(5, name);
                ps.setString(6, description);
                ps.setString(7, formattedType.name());
                ps.setString(8, formattedRegion);
                ps.setString(9, specsJson);

                int rows = ps.executeUpdate();
                if (rows > 0){
                    currentIngestion.offersStored++;
                }
            }

            try (PreparedStatement ps =conn.prepareStatement(upsertPriceSql)) {

                ps.setString(1, offerId);
                ps.setString(2, currency);
                ps.setBigDecimal(3, nu.price);
                ps.setString(4, nu.unit);

                int rows = ps.executeUpdate();
                if (rows > 0) currentIngestion.pricesStored++;
            }

        } catch (SQLException e) {
            Logger.LogError(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private OfferType formatOfferType(JsonObject obj) {
        String rawType = JsonToString(obj,"type");

        String meterName = firstNonBlank(JsonToString(obj, "meterName"),"").toLowerCase();
        String product = firstNonBlank(JsonToString(obj, "productName"),"").toLowerCase();
        String meterCat = firstNonBlank(JsonToString(obj, "meterCategory"),"").toLowerCase();

        if(meterCat.contains("network") || product.contains("bandwidth")|| meterName.contains("data transfer") ){
            return OfferType.DATA_TRANSFER;
        }

        if(rawType.contains("devtest")) {
            return OfferType.ON_DEMAND;
        }
        if(rawType.contains("consumption")) {
            return OfferType.ON_DEMAND;
        }
        if(rawType.contains("reservation") || product.contains("reservation")) {
            return OfferType.RESERVED;
        }
        if (meterName.contains("spot") || product.contains("spot")) {
            return OfferType.SPOT;
        }
        return OfferType.UNKNOWN;
    }

    private static String firstNonBlank(String... values) {
        for (String val: values){
            if(!val.isEmpty()){
                return val;
            }
        }
        return "";
    }

    private static String JsonToString(JsonObject json, String text) {
        if(json == null || json.isEmpty()){return null;}
        try {
            return json.get(text).getAsString();
        } catch (Exception e) {
            Logger.LogError(e.getMessage().toString());
            return null;
        }
    }


    private static BigDecimal getBigDecimal(JsonObject o, String key) {
        if (o == null ||
                !o.has(key) ||
                o.get(key).isJsonNull())
            return null;
        try {
            return o.get(key).getAsBigDecimal();
        } catch (Exception e) {
            try {
                return new BigDecimal(o.get(key).getAsString());
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private String safe(String s){
        return s.replaceAll("[^a-zA-Z0-9]+", "-");
    }

    private JsonObject fetchAPI(String url) {
        Logger.Log("[FETCH MONITOR-AZURE] "+url);
        try {
            byte[] bytes = httpClient.get().uri(URI.create(url)).retrieve().body(byte[].class);
            if (bytes == null || bytes.length == 0) {
                Logger.LogError("JSON READ FAIL");
                return null;
            }
            String raw = new String(bytes, StandardCharsets.UTF_8);

            Logger.Log("Downloaded " + bytes.length + " bytes:  " + url);

            return JsonParser.parseString(raw).getAsJsonObject();

        } catch (Exception e) {Logger.LogError("Json failed" + e.getMessage());return null;
        }
    }
}
