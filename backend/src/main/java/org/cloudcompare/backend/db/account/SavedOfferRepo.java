package org.cloudcompare.backend.db.account;

import org.cloudcompare.backend.util.Logger;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.*;

@Component
public class SavedOfferRepo {

    private final DataSource dataSource;

    public SavedOfferRepo(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void save(UUID userId, String offerId) {
        String sql = """
            INSERT INTO users_saved_offers (user_id, offer_id)
            VALUES (?, ?)
            ON CONFLICT DO NOTHING
        """;

        try (var connection = dataSource.getConnection();
             var ps = connection.prepareStatement(sql)) {

            ps.setObject(1, userId);
            ps.setString(2, offerId);
            ps.executeUpdate();

        } catch (Exception e) {
            Logger.LogError(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void delete(UUID userId, String offerId) {
        String sql = """
            DELETE FROM users_saved_offers
            WHERE user_id = ? AND offer_id = ?
        """;

        try (var c = dataSource.getConnection();
             var ps = c.prepareStatement(sql)) {

            ps.setObject(1, userId);
            ps.setString(2, offerId);
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Map<String, Object>> list(UUID userId) {
        String sql = """
            SELECT
              so.id,
              so.provider,
              so.service_id,
              so.region,
              so.name,
              p.price,
              p.unit,
              p.currency
            FROM users_saved_offers uso
            JOIN service_offers so ON so.id = uso.offer_id
            LEFT JOIN prices p ON p.offer_id = so.id
            WHERE uso.user_id = ?
            ORDER BY so.provider, so.region
        """;

        List<Map<String, Object>> out = new ArrayList<>();

        try (var c = dataSource.getConnection();
             var ps = c.prepareStatement(sql)) {

            ps.setObject(1, userId);

            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("offerId", rs.getString("id"));
                    row.put("provider", rs.getString("provider"));
                    row.put("serviceId", rs.getString("service_id"));
                    row.put("region", rs.getString("region"));
                    row.put("price", rs.getObject("price"));
                    row.put("unit", rs.getString("unit"));
                    row.put("currency", rs.getString("currency"));
                    out.add(row);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return out;
    }
}
