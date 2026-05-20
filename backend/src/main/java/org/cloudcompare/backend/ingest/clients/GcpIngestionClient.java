package org.cloudcompare.backend.ingest.clients;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.cloudcompare.backend.catalog.OfferType;
import org.cloudcompare.backend.db.DataTransferRepo;
import org.cloudcompare.backend.ingest.Ingestion;
import org.cloudcompare.backend.ingest.IngestionManager;
import org.cloudcompare.backend.ingest.util.RegionMapper;
import org.cloudcompare.backend.ingest.util.ServiceMapper;
import org.cloudcompare.backend.ingest.util.UnitNormaliser;
import org.cloudcompare.backend.util.Logger;
import org.cloudcompare.backend.catalog.Provider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;

@Component
public class GcpIngestionClient {
    private static final String BASE_URL = "https://cloudbilling.googleapis.com/v1/services/";

    @Value("${gcp.api.key}")
    private String API_KEY;

    private static final String GCP_COMPUTE_SERVICE_ID ="6F81-5844-456A";
    private static final String GCP_STORAGE_SERVICE_ID = "95FF-2EF5-5EA1";
    private static final String GCP_CLOUDSQL_SERVICE_ID = "9662-B51E-5089";

    private static final int MAX_ITEMS = 900000;

    private final RestClient httpClient;
    private final DataSource dataSource;

    public Ingestion currentIngestion;
    public DataTransferRepo dataTransferRepo;

    public IngestionManager ingestionManager;

    public GcpIngestionClient(RestClient.Builder httpBuilder, DataSource source,DataTransferRepo dataTransferRepo,IngestionManager ingestionManager) {
        this.dataSource = source;
        this.dataTransferRepo = dataTransferRepo;
        this.ingestionManager = ingestionManager;
        this.httpClient = httpBuilder.build();
    }

    public Ingestion ingestAll() {
        currentIngestion = new Ingestion();
        currentIngestion.running = true;
        currentIngestion.provider = Provider.GCP;
        ingestionManager.register(currentIngestion);

        ingestSingleService(GCP_COMPUTE_SERVICE_ID);
        ingestSingleService(GCP_STORAGE_SERVICE_ID);
        ingestSingleService(GCP_CLOUDSQL_SERVICE_ID);

        currentIngestion.running = false;
        currentIngestion.finishTime = Instant.now();
        return currentIngestion;
    }

    public Ingestion ingestCompute() {
        return ingestOnly(GCP_COMPUTE_SERVICE_ID);
    }

    public Ingestion ingestStorage() {
        return ingestOnly(GCP_STORAGE_SERVICE_ID);
    }

    public Ingestion ingestCloudSql() {
        return ingestOnly(GCP_CLOUDSQL_SERVICE_ID);
    }

    private Ingestion ingestOnly(String serviceId) {
        currentIngestion = new Ingestion();
        currentIngestion.running = true;
        currentIngestion.provider = Provider.GCP;

        ingestSingleService(serviceId);

        currentIngestion.running = false;
        currentIngestion.finishTime = Instant.now();
        return currentIngestion;
    }

    private void ingestSingleService(String gcpServiceId) {
        if (currentIngestion.offersStored >= MAX_ITEMS) return;

        String mappedService = ServiceMapper.mapServiceId(gcpServiceId,Provider.GCP);

        if(mappedService==null){
            Logger.LogError("GCP ingestion failed: no mapping serviceId=" + gcpServiceId);
            currentIngestion.recordError("No service mapping for gcpServiceId="+gcpServiceId);
            return;
        }

        String url = BASE_URL + gcpServiceId + "/skus?pageSize=5000";

        while (url != null &&currentIngestion.offersStored < MAX_ITEMS) {
            JsonObject root = fetchAPI(url);

            //error handling
            if(root == null) {
                currentIngestion.recordError("Failed to fetch GCP skus for serviceId="+gcpServiceId);
                return;
            }

            JsonArray skuArray;
            if(root.has("skus")&&root.get("skus").isJsonArray()){
                skuArray = root.getAsJsonArray("skus");
            }
            else{
                skuArray = new JsonArray();
            }

            for (JsonElement element : skuArray) {
                if (currentIngestion.offersStored >= MAX_ITEMS) break;

                if(element.isJsonObject()){
                    JsonObject sku = element.getAsJsonObject();
                    insertOfferAndPrice(mappedService,gcpServiceId,sku);
                }
            }

            String token = null;
            if(root.has("nextPageToken")){
                if(!root.get("nextPageToken").isJsonNull()){
                    token = root.get("nextPageToken").getAsString();
                }
            }

            if(token!=null&&!token.isBlank()){

                String encoded_token = "";
                try{
                    encoded_token = URLEncoder.encode(token, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    Logger.LogError("Failed to encode token:  + token");
                    return;
                }
                url = BASE_URL + gcpServiceId+"/skus?pageSize=5000&pageToken="+ encoded_token;
            }
            else{
                url = null;
            }
        }
    }

    private void insertOfferAndPrice(String canonicalServiceId, String gcpServiceId, JsonObject object) {
        String skuId = getStrFromJson(object, "skuId");
        if (skuId == null || skuId.isBlank()) return;

        String safeSKU = safe(skuId);
        String rawRegion = filterRegions(object);

        String canonicalisedRegion = RegionMapper.mapRegionName(rawRegion, Provider.GCP);

        String offerId = "offer-gcp-"+canonicalisedRegion+"-"+safeSKU;
        String provider = "GCP";
        String sku = skuId;

        String description = firstNonBlank(getStrFromJson(object, "description"));

        String name =getStrFromJson(object, "name");

        PricePoint priceData = extractPrice(object);

        String unit = "1";
        if(priceData.unit !=null){
            unit = priceData.unit;
        }


        String currency = "USD";
        if(priceData.currency!=null) currency = priceData.currency;

        BigDecimal price = BigDecimal.ZERO;
        if(priceData.price!=null){
            price = priceData.price;
        }

        UnitNormaliser.NormalizedUnit nu = UnitNormaliser.normalise(Provider.GCP, unit, price);

        OfferType type = mapGcpOfferType(object);
        currentIngestion.incrementOfferType(type);

        JsonObject specs = new JsonObject();
        if(rawRegion!=null){
            specs.addProperty("raw_region_code", rawRegion);
        }
        else{
            specs.addProperty("raw_region_code", "");
        }


        specs.addProperty("gcp_service_id", gcpServiceId);
        specs.addProperty("resource_group", GetEmbeddedStr(object, "category", "resourceGroup"));
        specs.addProperty("resource_family", GetEmbeddedStr(object, "category", "resourceFamily"));
        specs.addProperty("usage_type", GetEmbeddedStr(object, "category", "usageType"));
        specs.addProperty("geo_type", GetEmbeddedStr(object, "geoTaxonomy", "type"));
        specs.addProperty("raw_unit", unit);
        specs.addProperty("raw_price", price.toPlainString());
        specs.addProperty("normalized_unit", nu.unit);
        specs.addProperty("normalized_price", nu.price.toString());

        if (nu.price.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        String specsJson = specs.toString();
        upsertOfferAndPrice(offerId, canonicalServiceId, provider, sku, name, description, type.name(), canonicalisedRegion, specsJson, currency, nu.price, nu.unit);
    }

    private String safe(String s){
        return s.replaceAll("[^a-zA-Z0-9]+", "-");
    }

    private void upsertOfferAndPrice(String offerId, String serviceId, String provider, String sku, String name, String description, String type, String region, String specsJson, String currency, BigDecimal price, String unit)
    {
        String upsertOfferSql = """
                INSERT INTO service_offers
                (id, service_id, provider, sku, name, description, offer_type, region, specs)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
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
            try (PreparedStatement ps =
                         conn.prepareStatement(upsertOfferSql)) {

                ps.setString(1, offerId);
                ps.setString(2, serviceId);
                ps.setString(3, provider);
                ps.setString(4, sku);
                ps.setString(5, name);
                ps.setString(6, description);
                ps.setString(7, type);
                ps.setString(8, region);
                ps.setString(9, specsJson);

                int rows = ps.executeUpdate();
                if (rows > 0) currentIngestion.offersStored++;
            }

            try (PreparedStatement ps = conn.prepareStatement(upsertPriceSql)) {

                ps.setString(1, offerId);
                ps.setString(2, currency);
                ps.setBigDecimal(3, price);
                ps.setString(4, unit);

                int rows = ps.executeUpdate();
                if (rows > 0) currentIngestion.pricesStored++;
            }

        } catch (SQLException e) {
            Logger.LogError("SQL Error [GCP INGESTION CLIENT]: " +e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private OfferType mapGcpOfferType(JsonObject object) {
        String rf = firstNonBlank(GetEmbeddedStr(object, "category", "resourceFamily"), "").toLowerCase();
        String rg = firstNonBlank(GetEmbeddedStr(object, "category", "resourceGroup"), "").toLowerCase();
        String usageType = firstNonBlank(GetEmbeddedStr(object, "category", "usageType"), "").toLowerCase();

        String description = firstNonBlank(getStrFromJson(object, "description"), "").toLowerCase();

        if (rf.equals("network") || rg.contains("ingress") || rg.contains("egress") || description.contains("data transfer") || description.contains("ingress")){
            return OfferType.DATA_TRANSFER;
        }

        if (description.contains("operation") || description.contains("api")){
            return OfferType.API_REQUEST;
        }

        if (usageType.contains("commit") || usageType.contains("reserved")) {
            return OfferType.RESERVED;
        }

        if (description.contains("preemptible") || description.contains("spot")||usageType.contains("SPOT")) {
            return OfferType.SPOT;
        }

        if (usageType.equals("ondemand")) {
            return OfferType.ON_DEMAND;
        }

        return OfferType.UNKNOWN;
    }

    private JsonObject fetchAPI(String url) {
        Logger.Log("[FETCH MONITOR-GCP]"+url);
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

    private static String filterRegions(JsonObject object) {

        if (object.has("serviceRegions") && object.get("serviceRegions").isJsonArray()) {

            JsonArray array = object.getAsJsonArray("serviceRegions");
            if (!array.isEmpty() && array.get(0).isJsonPrimitive()) {
                String result = array.get(0).getAsString();
                if (!result.isBlank()) {
                    return result;
                }
            }
        }
        return "global";
    }

    private static PricePoint extractPrice(JsonObject object) {

        PricePoint price = new PricePoint();
        price.currency = "USD";
        price.unit = "1";

        if (!object.has("pricingInfo") || !object.get("pricingInfo").isJsonArray()){
            price.price = null;
            return price;
        }
        JsonArray pricingInfo = object.getAsJsonArray("pricingInfo");

        if (pricingInfo.isEmpty()) return price;

        if(pricingInfo.get(0).getAsJsonObject() ==null){
            return price;
        }

        JsonObject pricingExpression = pricingInfo.get(0).getAsJsonObject().getAsJsonObject("pricingExpression");
        if (pricingExpression == null) return price;

        price.unit = getStrFromJson(pricingExpression, "usageUnit");
        if(price.unit == null) price.unit = "1";

        if (!pricingExpression.has("tieredRates") || !pricingExpression.get("tieredRates").isJsonArray()) {
            return price;
        }

        JsonArray tiers = pricingExpression.getAsJsonArray("tieredRates");
        if (tiers.isEmpty()) return price;

        if(!tiers.get(0).isJsonObject() || tiers.get(0).getAsJsonObject() == null){
            return price;
        }

        JsonObject unitPrice = tiers.get(0).getAsJsonObject().getAsJsonObject("unitPrice");

        if (unitPrice == null) return price;

        String currencyCode = getStrFromJson(unitPrice, "currencyCode");

        if (currencyCode != null){
            price.currency = currencyCode;
        }

        BigDecimal units = BigDecimal.ZERO;
        BigDecimal nanos = BigDecimal.ZERO;

        String unitsStr = getStrFromJson(unitPrice, "units");

        if (unitsStr != null) {
            try {
                units = new BigDecimal(unitsStr);
            } catch (Exception ignored) {
                Logger.LogError("Error with decimilaisation of unit");
            }
        }

        if (unitPrice.has("nanos") && !unitPrice.get("nanos").isJsonNull()) {
            try {
                nanos = new BigDecimal(unitPrice.get("nanos").getAsLong());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        price.price = units.add(nanos.divide(new BigDecimal("1000000000")));

        return price;
    }

    private static String GetEmbeddedStr(JsonObject root, String key, String field) {
        if(root == null || !root.has(key)||root.get(key).isJsonNull()) return "";

        JsonObject object = root.getAsJsonObject(key);
        return firstNonBlank(getStrFromJson(object, field), "");
    }

    private static String firstNonBlank(String... vals) {
        for (String v : vals)
            if (v != null && !v.isBlank()) return v;
        return "";
    }

    private static String getStrFromJson(JsonObject object, String text) {
        if (object == null ||
                !object.has(text) ||
                object.get(text).isJsonNull())
            return null;
        try {
            return object.get(text).getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String encode(String s) {
        try {
            return URLEncoder.encode(
                    s,
                    StandardCharsets.UTF_8
            );
        } catch (Exception e) {
            return s;
        }
    }

    private static final class PricePoint {
        String currency;
        BigDecimal price;
        String unit;
    }
}
