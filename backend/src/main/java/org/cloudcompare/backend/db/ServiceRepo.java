package org.cloudcompare.backend.db;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.cloudcompare.backend.catalog.Service;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
public class ServiceRepo {
    private DataSource dataSource;
    private final Environment environment;

    public ServiceRepo(DataSource dataSource, Environment env) {
        this.dataSource = dataSource;
        environment = env;
    }

    public List<Service> retrieveAll(){
        String sql = """
            SELECT id, name, category, description
            FROM services
            """;
        List<Service> services = new ArrayList<>();

        try(Connection connection = dataSource.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            ResultSet resultSet = preparedStatement.executeQuery()){
            while(resultSet.next()){
                services.add(toService(resultSet));
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return services;
    }

    public Service retrieveById(String id){
        String sql = """
            SELECT id, name, category, description
            FROM services
            WHERE id = ?
            """;
        //Fix these stupid try catch formats
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, id);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) return toService(rs);
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteAll(){
        String sql = """
            DELETE from services
            """;
        List<Service> services = new ArrayList<>();
        try{
            Connection connection = dataSource.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.execute();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Service toService(ResultSet set) throws SQLException {
        return new Service(
                set.getString("id"),
                set.getString("name"),
                set.getString("category"),
                set.getString("description")
        );
    }
}