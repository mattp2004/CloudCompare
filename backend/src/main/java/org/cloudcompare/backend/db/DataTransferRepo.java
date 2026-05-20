package org.cloudcompare.backend.db;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.cloudcompare.backend.catalog.DataTransfer;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class DataTransferRepo {
    private final DataSource dataSource;

    private final Gson gson = new Gson();

    public DataTransferRepo(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<DataTransfer> retrieveDataTransfer(String provider, String type, String serviceId)
    {
        if (serviceId == null || serviceId.isBlank()) {
            serviceId = "svc-compute";
        }

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();

        if (provider != null && !provider.isBlank()) {
            where.append(" AND dt.provider = ? ");
            args.add(provider.trim());
        }

        if (type != null && !type.isBlank()) {
            where.append(" AND dt.type = ? ");
            args.add(type.trim());
        }

        where.append(" AND dt.service_id = ? ");
        args.add(serviceId.trim());

        String dataSQL = """
                SELECT dt.offer_id, dt.service_id, dt.provider, dt.type, dt.from_location, dt.to_location, dt.from_region, dt.to_region, dt.raw_specs, p.unit, p.price
                FROM network_transfers dt
                JOIN prices p ON dt.offer_id = p.offer_id
                """ + where + " ORDER BY p.price ASC ";
        String dataSQL2 = """
    SELECT 
        dt.service_id, 
        dt.provider, 
        dt.type, 
        dt.from_location, 
        dt.to_location, 
        dt.from_region, 
        dt.to_region, 
        dt.raw_specs, 
        p.unit, 
        MIN(p.price) AS price
    FROM network_transfers dt
    JOIN prices p ON dt.offer_id = p.offer_id
    """ + where + """
    GROUP BY 
        dt.service_id, 
        dt.provider, 
        dt.type, 
        dt.from_location, 
        dt.to_location, 
        dt.from_region, 
        dt.to_region, 
        dt.raw_specs, 
        p.unit
    ORDER BY price ASC
""";

        List<DataTransfer> dataTransfers = new ArrayList<>();

        try (Connection connection = dataSource.getConnection()) {
            try(PreparedStatement preparedStatement = connection.prepareStatement(dataSQL2)) {
                for (int i = 0; i < args.size(); i++) {
                    preparedStatement.setObject(i + 1, args.get(i));
                }


                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
                    while (resultSet.next()) {
                        DataTransfer dataTransfer = new DataTransfer();

                        dataTransfer.serviceId = resultSet.getString("service_id");
                        dataTransfer.provider = resultSet.getString("provider");
                        dataTransfer.type = resultSet.getString("type");

                        dataTransfer.fromLocation = resultSet.getString("from_location");
                        dataTransfer.toLocation = resultSet.getString("to_location");

                        dataTransfer.fromRegion = resultSet.getString("from_region");
                        dataTransfer.toRegion = resultSet.getString("to_region");

                        String rawJson = resultSet.getString("raw_specs");
                        dataTransfer.specsJson = gson.fromJson(rawJson, mapType);

                        dataTransfer.price = resultSet.getBigDecimal("price");
                        dataTransfer.unit = resultSet.getString("unit");

                        dataTransfers.add(dataTransfer);
                    }
                }
            }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        return dataTransfers;
    }

    public void upsertDataTransfer(
            String offerId,
            String serviceId,
            String provider,
            String type,
            String fromLocation,
            String toLocation,
            String fromRegion,
            String toRegion,
            String rawSpecs
    ) {

        String sql = """
        INSERT INTO network_transfers
        (offer_id, service_id, provider, type, from_location, to_location, from_region, to_region, raw_specs)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
        ON CONFLICT (offer_id)
        DO UPDATE SET
            service_id = EXCLUDED.service_id,
            provider = EXCLUDED.provider,
            type = EXCLUDED.type,
            from_location = EXCLUDED.from_location,
            to_location = EXCLUDED.to_location,
            from_region = EXCLUDED.from_region,
            to_region = EXCLUDED.to_region,
            raw_specs = EXCLUDED.raw_specs
        """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, offerId);
            ps.setString(2, serviceId);
            ps.setString(3, provider);
            ps.setString(4, type);
            ps.setString(5, fromLocation);
            ps.setString(6, toLocation);
            ps.setString(7, fromRegion);
            ps.setString(8, toRegion);
            ps.setString(9, rawSpecs);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
