package org.cloudcompare.backend.db.offers;

import org.cloudcompare.backend.catalog.Price;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class PriceRepo {
    private DataSource dataSource;
    private final Environment environment;

    public PriceRepo(DataSource dataSource, Environment env) {
        this.dataSource = dataSource;
        environment = env;
    }

    //Retrieve based on service id

    public Price retrieveByOffer(String id){
        String sql = """
            SELECT offer_id, currency, price, unit
            FROM prices
            WHERE offer_id = ?
            """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, id);

            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) return toPrice(rs);
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Price toPrice(ResultSet set) throws SQLException {
        return new Price(
                set.getString("offer_id"),
                set.getString("currency"),
                set.getBigDecimal("price"),
                set.getString("unit")
        );
    }
}
