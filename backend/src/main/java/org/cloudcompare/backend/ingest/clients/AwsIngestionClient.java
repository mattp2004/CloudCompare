package org.cloudcompare.backend.ingest.clients;

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
import java.util.ArrayList;
import java.util.List;

@Component
public class AwsIngestionClient {

    private static String BASE_URL =
            "https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/index.json";
    private static final String AWS_RDS_INDEX =
            "https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AmazonRDS/current/index.json";
    private static final String AWS_S3_INDEX =
            "https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AmazonS3/current/index.json";

    private static final String AWS_EC2_REGIONAL_INDEX =
            "https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AmazonEC2/current/%s/index.json";

    private static final List<String> AWS_EC2_REGIONS = List.of("us-east-1", "us-east-2", "us-west-1", "us-west-2",
            "eu-west-1", "eu-west-2", "eu-west-3", "eu-central-1",
            "ap-southeast-1", "ap-southeast-2", "ap-northeast-1", "ap-northeast-2",
            "ap-south-1", "sa-east-1", "ca-central-1"
    );

    private static final int MAX_ITEMS = 900000;

    private final RestClient httpClient;
    private final DataSource dataSource;

    public Ingestion currentIngestion;
    public IngestionManager ingestionManager;

    AwsIngestionClient(RestClient.Builder httpBuilder, DataSource source, IngestionManager ingestionManager) {
        this.httpClient = httpBuilder.build();
        this.dataSource = source;
        this.ingestionManager = ingestionManager;
    }

    public Ingestion ingestRds() {
        return ingestSingleService("AmazonRDS", AWS_RDS_INDEX);
    }

    public Ingestion ingestS3() {
        return ingestSingleService("AmazonS3", AWS_S3_INDEX);
    }

    public Ingestion ingestEc2() {
        currentIngestion = new Ingestion();
        currentIngestion.running = true;
        currentIngestion.provider = Provider.AWS;
        ingestionManager.register(currentIngestion);

        String mappedServiceId = ServiceMapper.mapServiceName("AmazonEC2", Provider.AWS);
        if (mappedServiceId == null) {
            Logger.LogError("Ingestion failed. No mapping for: AmazonEC2");
            currentIngestion.recordError("No mapping for serviceCode = AmazonEC2");
            currentIngestion.running = false;
            currentIngestion.finishTime = Instant.now();
            return currentIngestion;
        }

        Logger.Log("Mapping service code AmazonEC2 -> " + mappedServiceId);

        for (String region : AWS_EC2_REGIONS) {
            if (currentIngestion.offersStored >= MAX_ITEMS) break;

            String url = AWS_EC2_REGIONAL_INDEX.formatted(region);
            Logger.Log("Starting AWS EC2 ingestion for region: " + region);

            JsonObject serviceIndex = fetchAPI(url);
            if (serviceIndex == null) {
                currentIngestion.recordError("Failed to fetch EC2 index for region=" + region);
                Logger.LogError("Failed to fetch EC2 index for region=" + region);
                continue;
            }

            ingestService("AmazonEC2", serviceIndex);
        }

        currentIngestion.running = false;
        currentIngestion.finishTime = Instant.now();
        return currentIngestion;
    }

    private Ingestion ingestSingleService(String serviceCode, String indexUrl) {
        currentIngestion = new Ingestion();

        currentIngestion.running = true;
        currentIngestion.provider = Provider.AWS;
        ingestionManager.register(currentIngestion);

        String mappedServiceId = ServiceMapper.mapServiceName(serviceCode, Provider.AWS);

        //Handling service mapping error
        if (mappedServiceId == null) {
            Logger.LogError("Ingestion failed. No mapping for: " + serviceCode);
            currentIngestion.recordError("No mapping for serviceCode = " + serviceCode);
            currentIngestion.running = false;
            currentIngestion.finishTime = Instant.now();
            return currentIngestion;
        }

        Logger.Log("Mapping service code " + serviceCode + " -> " + mappedServiceId);

        JsonObject serviceIndex = fetchAPI(indexUrl);

        if (serviceIndex == null) {
            Logger.LogError("AWS single-service ingest failed: could not fetch index " + indexUrl);
            currentIngestion.recordError("Failed to fetch indexUrl=" + indexUrl);
            currentIngestion.running = false;
            currentIngestion.finishTime = Instant.now();
            return currentIngestion;
        }

        ingestService(serviceCode, serviceIndex);

        currentIngestion.running = false;
        currentIngestion.finishTime = Instant.now();
        return currentIngestion;
    }

    public Ingestion ingestAll() {
        currentIngestion = new Ingestion();
        currentIngestion.running = true;
        currentIngestion.provider = Provider.AWS;
        ingestionManager.register(currentIngestion);

        JsonObject rootObject = fetchAPI(BASE_URL);
        if (rootObject == null) {
            Logger.LogError("AWS Ingestion failed could not find root object.");
            currentIngestion.finishTime = Instant.now();
            currentIngestion.running = false;
            return currentIngestion;
        }

        JsonObject offers;
        if (rootObject.has("offers") &&
                rootObject.get("offers").isJsonObject()) {
            offers = rootObject.getAsJsonObject("offers");
        } else {
            offers = new JsonObject();
        }

        List<String> ingestionData = new ArrayList<>();

        for (String serviceCode : offers.keySet()) {
            String mapped = ServiceMapper.mapServiceName(serviceCode, Provider.AWS);
            if (mapped != null) {
                ingestionData.add(serviceCode);
            } else {
                Logger.LogError("Failed to map service name: " + serviceCode);
            }
        }

        for(int i = 0; i < ingestionData.size(); i++){
            String current = ingestionData.get(i);
            if(currentIngestion.offersStored>=MAX_ITEMS){
                Logger.Log("Reached ingestion limit [AWS] " + MAX_ITEMS);
                break;
            }

            JsonObject offer = offers.getAsJsonObject(current);
            if(offer.isEmpty()||offer == null) continue;

            String currentVrslUrl = JsonToString(offer,"currentVersionUrl");
            if(currentVrslUrl == null || currentVrslUrl.isBlank()){
                continue;
            }

            //safe guard
            if(!currentVrslUrl.startsWith("http://")){
                currentVrslUrl="https://pricing.us-east-1.amazonaws.com" +currentVrslUrl;
            }

            JsonObject serviceIndex = fetchAPI(currentVrslUrl);
            if(serviceIndex == null ||serviceIndex.isEmpty()){
                currentIngestion.recordError("Failed to fetch aws service index: " + current);
                Logger.Log("Failed to fetch aws service index: " + current);
                continue;
            }
            Logger.Log("Ingesting AWS service code " + current + "mapped to " + ServiceMapper.mapServiceName(current, Provider.AWS));
            ingestService(current, serviceIndex);
        }
        currentIngestion.running = false;
        currentIngestion.finishTime = Instant.now();
        return currentIngestion;
    }

    private void ingestService(String serviceCode, JsonObject serviceIndex) {
        String formattedServiceName = ServiceMapper.mapServiceName(serviceCode, Provider.AWS);

        JsonObject products;
        if (serviceIndex.has("products") && serviceIndex.get("products").isJsonObject()) {
            products = serviceIndex.getAsJsonObject("products");
        } else {
            products = new JsonObject();
        }

        JsonObject terms;
        if (serviceIndex.has("terms") && serviceIndex.get("terms").isJsonObject()) {
            terms = serviceIndex.getAsJsonObject("terms");
        }
        else {
            terms = new JsonObject();
        }

        JsonObject onDemand;
        if (terms.has("OnDemand") && terms.get("OnDemand").isJsonObject()) {
            onDemand = terms.getAsJsonObject("OnDemand");
        } else {
            onDemand = new JsonObject();
        }

        for (String sku : products.keySet()) {
            if (currentIngestion.offersStored >= MAX_ITEMS) {
                break;
            }

            JsonObject product = products.getAsJsonObject(sku);
            if (product == null || product.isJsonNull()) continue;

            JsonObject attributes;
            if (product.has("attributes") && product.get("attributes").isJsonObject()) {
                attributes = product.getAsJsonObject("attributes");
            } else {
                attributes = new JsonObject();
            }

            String regionCode = JsonToString(attributes, "regionCode");
            String location = JsonToString(attributes, "location");
            String formattedRegion = RegionMapper.mapRegionName(regionCode, Provider.AWS);
            String productFamily = firstNonBlank(JsonToString(product, "productFamily"), "unknown");
            String safeType = productFamily.replaceAll("[^a-zA-Z0-9]+", "-").toLowerCase();
            String safeSKU = sku.replaceAll("[^a-zA-Z0-9]+", "-");
            String offerId = "offer-aws-" + formattedRegion + "-" + safeType + "-" + safeSKU;

            BigDecimal price = null;String currency = "USD";String unit = "1";

            JsonObject skuTerms;
            JsonObject term;
            JsonObject priceDimensions;
            JsonObject dim;
            JsonObject pricePerUnit;

            if (onDemand.has(sku) && onDemand.get(sku).isJsonObject()) {skuTerms = onDemand.getAsJsonObject(sku);
            } else {
                skuTerms = new JsonObject();
            }

            if (skuTerms != null && !skuTerms.isJsonNull() && !skuTerms.keySet().isEmpty()) {
                term = skuTerms.getAsJsonObject(skuTerms.keySet().iterator().next());

                if(term != null && term.has("priceDimensions")&&term.get("priceDimensions").isJsonObject()){
                    priceDimensions = term.getAsJsonObject("priceDimensions");
                }
                else{
                    priceDimensions = null;
                }
                if (priceDimensions != null && !priceDimensions.keySet().isEmpty()) {
                    dim = priceDimensions.getAsJsonObject(priceDimensions.keySet().iterator().next());

                    unit = firstNonBlank(JsonToString(dim, "unit"), unit);
                    if(dim!=null&&dim.has("pricePerUnit")&& dim.get("pricePerUnit").isJsonObject()){
                        pricePerUnit = dim.getAsJsonObject("pricePerUnit");
                    }
                    else {pricePerUnit = null;}

                    if (pricePerUnit != null) {String raw_price = JsonToString(pricePerUnit, "USD");
                        if (raw_price != null) {
                            try {
                                price = new BigDecimal(raw_price);
                            } catch (Exception e) {
                                price = BigDecimal.ZERO;
                            }
                        }
                    }
                }
            }

            if (price == null) price = BigDecimal.ZERO;

            String name = firstNonBlank(JsonToString(attributes, "instanceType"),JsonToString(attributes, "usagetype"),JsonToString(product, "productFamily"),sku);

            String description = firstNonBlank(JsonToString(attributes, "servicecode"),JsonToString(attributes, "serviceCode"),JsonToString(attributes, "operation"),"-");

            JsonObject specs = new JsonObject();
            specs.addProperty("raw_region_code", regionCode);
            specs.addProperty("raw_location", firstNonBlank(JsonToString(attributes, "location"), ""));
            specs.addProperty("instance_type", firstNonBlank(JsonToString(attributes, "instanceType"), "-"));
            specs.addProperty("product_family", firstNonBlank(JsonToString(product, "productFamily"), ""));
            specs.addProperty("tenancy", firstNonBlank(JsonToString(attributes, "tenancy"), ""));
            specs.addProperty("operating_system", firstNonBlank(JsonToString(attributes, "operatingSystem"),""));
            specs.addProperty("database_engine", firstNonBlank(JsonToString(attributes, "databaseEngine"), ""));
            specs.addProperty("usage_type", firstNonBlank(JsonToString(attributes, "usagetype"), ""));
            specs.addProperty("raw_unit", unit);

            String specsJson = specs.toString();

            UnitNormaliser.NormalizedUnit nu = UnitNormaliser.normalise(Provider.AWS, unit, price);
            if (nu.price.compareTo(BigDecimal.valueOf(0)) <= 0) {
                continue;
            }

            OfferType type = mapAwsOfferType(attributes, productFamily);
            currentIngestion.incrementOfferType(type);

            upsertOfferAndPrice(offerId, formattedServiceName, "AWS", sku, name, description, type.name(), formattedRegion,specsJson, currency, nu.price, nu.unit
            );
        }
    }

    private OfferType mapAwsOfferType(JsonObject attributes, String productFamily) {
        String usageType =JsonToString(attributes, "usagetype").toLowerCase();
        String operation =JsonToString(attributes, "operation").toLowerCase();
        String family = productFamily.toLowerCase();

        if (usageType.contains("datatransfer") || family.contains("data transfer")) {
            return OfferType.DATA_TRANSFER;
        }

        if (usageType.contains("spot")) {return OfferType.SPOT;}

        if (usageType.contains("reserved") || operation.contains("reserved")) {
            return OfferType.RESERVED;
        }

        if (operation.contains("request") || family.contains("api") || family.contains("rest-request")) {
            return OfferType.API_REQUEST;
        }

        if (family.contains("compute instance") || family.contains("storage") || family.contains("database instance") || family.contains("compute")) {
            return OfferType.ON_DEMAND;
        }
        return OfferType.UNKNOWN;
    }

    private void upsertOfferAndPrice(String offerId,String serviceId,String provider,String sku,String name,String description,String type,String region, String specsJson,String currency, BigDecimal price, String unit) {

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

            try (PreparedStatement ps = conn.prepareStatement(upsertOfferSql)) {
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
            Logger.LogError(e.getMessage());
            throw new RuntimeException(e);
        }
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

    private JsonObject fetchAPI(String url) {
        Logger.Log("[FETCH MONITOR]"+url);
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