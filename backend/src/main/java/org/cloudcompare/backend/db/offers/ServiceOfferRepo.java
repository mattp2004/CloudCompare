package org.cloudcompare.backend.db.offers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.cloudcompare.backend.catalog.MapResults;
import org.cloudcompare.backend.catalog.OfferType;
import org.cloudcompare.backend.catalog.ResultsPage;
import org.cloudcompare.backend.catalog.ServiceOffer;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;

@Component
public class ServiceOfferRepo {

    private final DataSource dataSource;
    private final Environment environment;

    private final Gson gson = new Gson();
    private final Type mapType = new TypeToken<Map<String, Object>>() {}.getType();

    public ServiceOfferRepo(DataSource dataSource, Environment env) {
        this.dataSource = dataSource;
        this.environment = env;
    }


    public List<ServiceOffer> retrieveByServiceID(String serviceId) {
        String sql = """
            SELECT id, service_id, provider, sku, name, description, offer_type, region, specs
            FROM service_offers
            WHERE service_id = ?
            ORDER BY provider, region, sku
        """;

        List<ServiceOffer> offers = new ArrayList<>();

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, serviceId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    offers.add(toServiceOffer(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return offers;
    }



    public ResultsPage searchForOffers(
            String serviceId,
            String provider,
            String region,
            String type,
            String unit,
            String q,
            String orderBy,
            int page,
            int size
    ) {
        if (page < 1) page = 1;
        if (size < 1) size = 1;
        if (size > 200) size = 200;

        int offset = (page - 1) * size;

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();

        if (serviceId != null && !serviceId.isBlank()) {
            where.append(" AND service_id = ? ");
            args.add(serviceId.trim());
        }
        if (provider != null && !provider.isBlank()) {
            where.append(" AND provider = ? ");
            args.add(provider.trim().toUpperCase());
        }
        if (region != null && !region.isBlank()) {
            where.append(" AND region = ? ");
            args.add(region.trim());
        }
        if (type != null && !type.isBlank()) {
            where.append(" AND offer_type = ? ");
            args.add(type.trim());
        }
        if (unit != null && !unit.isBlank()) {
            where.append("""
    AND EXISTS(
        SELECT 1
        FROM prices p2
        WHERE p2.offer_id = service_offers.id
          AND p2.unit = ?
    )
""");
            args.add(unit.trim());
        }
        if (q != null && !q.isBlank()) {
            where.append(" AND (id ILIKE ? OR sku ILIKE ? OR name ILIKE ? OR description ILIKE ?) ");
            String like = "%" + q.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }

        String order = "provider, region, sku";
        String fromSql = "FROM service_offers";

        if(orderBy != null && !orderBy.isBlank()) {
            order = orderBy.trim();
            if(orderBy.equalsIgnoreCase("price")){
                order = "p.price ASC NULLS LAST, provider, region, sku";
                fromSql = "FROM service_offers LEFT JOIN prices p ON p.offer_id = service_offers.id";
            }
        }

        String countSql = "SELECT COUNT(*) FROM service_offers " + where;

        String dataSql = """
            SELECT id, service_id, provider, sku, name, description, offer_type, region, specs
        """ + fromSql + " " + where +
                " ORDER BY " + order +
                " LIMIT ? OFFSET ? ";

        long total = 0;
        List<ServiceOffer> items = new ArrayList<>();

        try (Connection c = dataSource.getConnection()) {

            try (PreparedStatement ps = c.prepareStatement(countSql)) {
                bind(ps, args);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) total = rs.getLong(1);
                }
            }

            List<Object> args2 = new ArrayList<>(args);
            args2.add(size);
            args2.add(offset);

            try (PreparedStatement ps = c.prepareStatement(dataSql)) {
                bind(ps, args2);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        items.add(toServiceOffer(rs));
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return new ResultsPage(page, size, total, items);
    }

    public List<MapResults> searchForMap(
            String serviceId,
            String provider,
            String region,
            String type) {
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();

        if (serviceId != null && !serviceId.isBlank()) {
            where.append(" AND o.service_id = ? ");
            args.add(serviceId.trim());
        }
        if (provider != null && !provider.isBlank()) {
            where.append(" AND o.provider = ? ");
            args.add(provider.trim().toUpperCase());
        }
        if (region != null && !region.isBlank()) {
            where.append(" AND o.region = ? ");
            args.add(region.trim());
        }
        if (type != null && !type.isBlank()) {
            where.append(" AND offer_type = ? ");
            args.add(type.trim());
        }

        String sql = """
            SELECT
              o.region,
              o.provider,
              o.service_id,
              COUNT(*) AS offer_count,
              p.currency,
              p.unit,
              MIN(p.price) AS min_price,
              AVG(p.price) AS avg_price
            FROM service_offers o
            LEFT JOIN prices p ON p.offer_id = o.id
        """ + where + """
            GROUP BY
              o.region, o.provider, o.service_id,
              p.currency, p.unit
            ORDER BY o.region, o.provider
        """;

        List<MapResults> out = new ArrayList<>();

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            bind(ps, args);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new MapResults(
                            rs.getString("region"),
                            rs.getString("provider"),
                            rs.getString("service_id"),
                            rs.getLong("offer_count"),
                            rs.getString("currency"),
                            rs.getString("unit"),
                            rs.getObject("min_price") == null ? null : rs.getDouble("min_price"),
                            rs.getObject("avg_price") == null ? null : rs.getDouble("avg_price")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return out;
    }

    private void bind(PreparedStatement ps, List<Object> args) throws SQLException {
        for (int i = 0; i < args.size(); i++) {
            Object v = args.get(i);
            if (v instanceof Integer) ps.setInt(i + 1, (Integer) v);
            else if (v instanceof Long) ps.setLong(i + 1, (Long) v);
            else ps.setString(i + 1, v.toString());
        }
    }

    private Map<String, Object> parseSpecs(String json) {
        if (json == null || json.isBlank()) return new HashMap<>();
        try {
            return gson.fromJson(json, mapType);
        } catch (Exception e) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("raw", json);
            return fallback;
        }
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

    public void clearOffers() {
        try (Connection c = dataSource.getConnection()) {
            c.prepareStatement("TRUNCATE TABLE prices").execute();
            c.prepareStatement("TRUNCATE TABLE service_offers").execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}