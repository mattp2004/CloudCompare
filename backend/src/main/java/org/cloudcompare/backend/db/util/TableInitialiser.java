package org.cloudcompare.backend.db.util;

import jakarta.annotation.PostConstruct;
import org.cloudcompare.backend.util.Logger;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

@Component
public class TableInitialiser {

    private DataSource dataSource;
    private final Environment environment;

    public TableInitialiser(DataSource dataSource, Environment env) {
        this.dataSource = dataSource;
        environment = env;
    }

    @PostConstruct
    public void initTables() {
        if (Arrays.asList(environment.getActiveProfiles()).contains("test")) {
            return;
        }
        try(
            Connection connection = dataSource.getConnection();
            Statement tableStatement = connection.createStatement()){
                tableStatement.execute("""
                        CREATE TABLE IF NOT EXISTS services (
                            id TEXT PRIMARY KEY,
                            name TEXT NOT NULL,
                            category TEXT NOT NULL,
                            description TEXT NOT NULL
                        )
                        """);
            tableStatement.execute("""
                        CREATE TABLE IF NOT EXISTS service_offers (
                            id TEXT PRIMARY KEY,
                            service_id TEXT NOT NULL,
                            provider TEXT NOT NULL,
                            sku TEXT NOT NULL,
                            name TEXT NOT NULL,
                            description TEXT NOT NULL,
                            offer_type TEXT NOT NULL,
                            region TEXT NOT NULL,
                            specs JSONB NOT NULL,
                            FOREIGN KEY (service_id) REFERENCES services(id)
                        )
                        """);

            tableStatement.execute("""
                        CREATE TABLE IF NOT EXISTS network_transfers (
                            id BIGSERIAL PRIMARY KEY,
                            offer_id TEXT NOT NULL UNIQUE,
                            service_id TEXT NOT NULL,
                            provider TEXT NOT NULL,
                            type TEXT NOT NULL,
                            from_location TEXT NOT NULL,
                            to_location TEXT NOT NULL,
                            from_region TEXT NOT NULL,
                            to_region TEXT NOT NULL,
                            raw_specs JSONB NOT NULL,
                            FOREIGN KEY (offer_id) REFERENCES service_offers(id)
                        )
                    """);

            tableStatement.execute("""
                        CREATE TABLE IF NOT EXISTS prices (
                            offer_id TEXT PRIMARY KEY,
                            currency TEXT NOT NULL,
                            price NUMERIC(18,8) NOT NULL,
                            unit TEXT NOT NULL,
                            FOREIGN KEY (offer_id) REFERENCES service_offers(id)
                        )
                        """);

            //User table
            tableStatement.execute("""
                        CREATE TABLE IF NOT EXISTS users (
                            user_id UUID PRIMARY KEY,
                            username TEXT UNIQUE NOT NULL,
                            email TEXT UNIQUE NOT NULL,
                            password_hash TEXT NOT NULL,
                            rank TEXT NOT NULL DEFAULT 'Default',
                            creation_date TIMESTAMP DEFAULT now()
                        )
                        """);

            //Saved offers table
            tableStatement.execute("""
                        CREATE TABLE IF NOT EXISTS users_saved_offers (
                            user_id UUID REFERENCES users(user_id),
                            offer_id TEXT REFERENCES service_offers(id),
                            PRIMARY KEY (user_id, offer_id)
                        )
                        """);



            Logger.Log("Completed table creation,");

        } catch (SQLException e) {
            Logger.LogError("Failed to create SQL tables: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}