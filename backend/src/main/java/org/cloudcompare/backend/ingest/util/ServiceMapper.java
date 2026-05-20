package org.cloudcompare.backend.ingest.util;

import org.cloudcompare.backend.catalog.Provider;

import java.util.Map;

public final class ServiceMapper {

    private static final Map<String, String> AzureServiceMapping = Map.of(
            "Virtual Machines", "svc-compute",
            "Storage", "svc-storage",
            "Azure Database for MySQL", "svc-db-mysql"
    );

    private static final Map<String, String> AwsServiceMapping = Map.of(
            "AmazonEC2", "svc-compute",
            "AmazonS3", "svc-storage",
            "AmazonRDS", "svc-db-mysql"
    );

    private static final Map<String, String> GcpServiceMapping = Map.of(
            "Compute Engine", "svc-compute",
            "Cloud Storage",  "svc-storage",
            "Cloud SQL",      "svc-db-mysql"
    );

    private static final Map<String, String> GcpServiceIdMapping = Map.of(
            "6F81-5844-456A", "svc-compute",
            "95FF-2EF5-5EA1", "svc-storage",
            "9662-B51E-5089", "svc-db-mysql"
    );

    public static String mapServiceName(String serviceName, Provider provider) {
        if (provider == Provider.AZURE) return AzureServiceMapping.get(serviceName);
        if (provider == Provider.AWS) return AwsServiceMapping.get(serviceName);
        if (provider == Provider.GCP) return GcpServiceMapping.get(serviceName);
        return null;
    }

    public static String mapServiceId(String serviceId, Provider provider) {
        if (provider == Provider.GCP) return GcpServiceIdMapping.get(serviceId);
        return null;
    }
}
