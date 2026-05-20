package org.cloudcompare.backend.db.offers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cloudcompare.backend.ai.AiComparisonService;
import org.cloudcompare.backend.ai.SbertEngine;
import org.cloudcompare.backend.ai.SbertTextNormaliser;
import org.cloudcompare.backend.catalog.Compare;
import org.cloudcompare.backend.catalog.OfferType;
import org.cloudcompare.backend.catalog.Price;
import org.cloudcompare.backend.catalog.ServiceOffer;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class CompareRepo {

    private final DataSource dataSource;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AiComparisonService ai;
    private final SbertEngine sbertEngine;

    public CompareRepo(DataSource dataSource, AiComparisonService ai, SbertEngine sbertEngine) {
        this.dataSource = dataSource;
        this.ai = ai;
        this.sbertEngine = sbertEngine;
    }

    public Compare makeComparisonById(String offerId, int maxLimit) {

        String selectedSql = """
            SELECT
              so.id, so.service_id, so.provider, so.sku, so.name, so.description, so.offer_type, so.region,
              so.specs::text AS specs_json,
              p.currency, p.price, p.unit
            FROM service_offers so
            JOIN prices p ON p.offer_id = so.id
            WHERE so.id = ?
            LIMIT 1
        """;

        String serviceId;
        String region;
        String unit;
        String currency;
        String offerType;

        ServiceOffer selectedOffer;
        Price selectedPrice;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(selectedSql)) {

            preparedStatement.setString(1, offerId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (!resultSet.next()) return null;

                selectedOffer = mapOffer(resultSet);
                selectedPrice = mapPrice(resultSet, selectedOffer.getId());

                serviceId = safe(resultSet.getString("service_id"));

                region = safe(resultSet.getString("region"));
                unit = safe(resultSet.getString("unit"));
                currency = safe(resultSet.getString("currency"));
                offerType = safe(resultSet.getString("offer_type"));

                if (currency.isBlank()) currency = "USD";
                if (unit.isBlank()) unit = "1";
            }
        } catch (Exception e) {
            throw new RuntimeException("CompareRepo selected query failed", e);
        }

        List<ServiceOffer> comparedOffers = new ArrayList<>();
        List<Price> comparedPrices = new ArrayList<>();

        String altSql = """
            SELECT
              so.id, so.service_id, so.provider, so.sku, so.name, so.description, so.offer_type, so.region,
              so.specs::text AS specs_json,
              p.currency, p.price, p.unit
            FROM service_offers so
            JOIN prices p ON p.offer_id = so.id
            WHERE so.service_id       = ?
              AND so.region           = ?
              AND so.offer_type       = ?
              AND p.unit              = ?
              AND p.currency          = ?
              AND so.id              <> ?
            ORDER BY p.price ASC
            LIMIT ?
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(altSql)) {

            ps.setString(1, serviceId);
            ps.setString(2, region);
            ps.setString(3,offerType);
            ps.setString(4, unit);
            ps.setString(5, currency);
            ps.setString(6, offerId);
            ps.setInt(7, Math.max(1, maxLimit));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ServiceOffer o = mapOffer(rs);
                    Price p = mapPrice(rs, o.getId());
                    comparedOffers.add(o);
                    comparedPrices.add(p);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("CompareRepo alternatives query failed", e);
        }

        return new Compare(
                selectedOffer,
                selectedPrice,
                serviceId,
                region,
                unit,
                currency,
                comparedOffers,
                comparedPrices
        );
    }

    private ServiceOffer mapOffer(ResultSet rs) throws Exception {

        String id = rs.getString("id");
        String serviceId = rs.getString("service_id");
        String provider = rs.getString("provider");
        String sku = rs.getString("sku");
        String name = rs.getString("name");
        String description = rs.getString("description");
        String type = rs.getString("offer_type");
        String region = rs.getString("region");
        String specsJson = rs.getString("specs_json");

        Map<String, Object> specsMap = parseSpecs(specsJson);

        return new ServiceOffer(
                safe(id),
                safe(serviceId),
                safe(provider),
                safe(sku),
                safe(name),
                safe(description),
                parseOfferType(type),
                safe(region),
                specsMap
        );
    }

    private OfferType parseOfferType(String raw) {
        if (raw == null || raw.isBlank()) {
            return OfferType.UNKNOWN;
        }

        try {
            return OfferType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return OfferType.UNKNOWN;
        }
    }

    private Price mapPrice(ResultSet rs, String offerId) throws SQLException {

        String currency = rs.getString("currency");
        BigDecimal price = rs.getBigDecimal("price");
        String unit = rs.getString("unit");

        if (currency == null || currency.isBlank()) currency = "USD";
        if (price == null) price = BigDecimal.ZERO;
        if (unit == null || unit.isBlank()) unit = "1";

        return new Price(offerId, currency, price, unit);
    }

    private Map<String, Object> parseSpecs(String specsJson) {

        if (specsJson == null || specsJson.isBlank()) return new HashMap<>();

        try {
            return mapper.readValue(specsJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            Map<String, Object> m = new HashMap<>();
            m.put("raw_specs", specsJson);
            return m;
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    public ServiceOffer getOfferById(String offerId) {
        String sql = """
        SELECT id, service_id, provider, sku, name, description, offer_type, region, specs
        FROM service_offers
        WHERE id = ?
        LIMIT 1
    """;

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, offerId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return toServiceOffer(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return null;
    }
    private ServiceOffer toServiceOffer(ResultSet rs) throws SQLException {
        return new ServiceOffer(
                rs.getString("id"),
                rs.getString("service_id"),
                rs.getString("provider"),
                rs.getString("sku"),
                rs.getString("name"),
                rs.getString("description"),
                parseOfferType(rs.getString("offer_type")),
                rs.getString("region"),
                parseSpecs(rs.getString("specs"))
        );
    }

    public Map<String, Object> aiComparison(String offerId, String offer2Id) {
        ServiceOffer a = getOfferById(offerId);
        ServiceOffer b = getOfferById(offer2Id);

        if (a == null|| b == null) {
            throw new RuntimeException("Offers not found");
        }

        double sbertScore = 0.0;
        try {
            String textA = SbertTextNormaliser.buildSbertText(a);
            String textB = SbertTextNormaliser.buildSbertText(b);
            sbertScore = sbertEngine.executeSbert(textA, textB);
        }catch (Exception e) {
            System.err.println("Failed SBERT");
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            String jsonA = mapper.writeValueAsString(a);
            String jsonB = mapper.writeValueAsString(b);
            String aiReport = ai.compareOffers(jsonA,jsonB);

            Map<String, Object> resultJson = new HashMap<>();
            resultJson.put("sbert", Math.round(sbertScore*100.0)/100.0);
            resultJson.put("sbertPercentage", (int)Math.round(sbertScore*100));
            resultJson.put("aiReport",aiReport);
            return resultJson;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }    }
}
