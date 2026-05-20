package org.cloudcompare.backend.ingest.util;

import org.cloudcompare.backend.catalog.Provider;
import org.cloudcompare.backend.util.UnmappedLogger;

import java.util.Map;

import static java.util.Map.entry;

public final class RegionMapper {

    private RegionMapper() {

    }

    private static final Map<String, String> AzureRegionMapping = Map.ofEntries(
            entry("uksouth", "UK"),
            entry("ukwest", "UK"),

            entry("westeurope", "EU-WEST"),
            entry("northeurope", "EU-NORTH"),
            entry("francecentral", "EU-WEST"),
            entry("francesouth", "EU-SOUTH"),
            entry("germanywestcentral", "EU-CENTRAL"),
            entry("germanynorth", "EU-NORTH"),
            entry("switzerlandnorth", "EU-CENTRAL"),
            entry("switzerlandwest", "EU-CENTRAL"),
            entry("norwayeast", "EU-NORTH"),
            entry("norwaywest", "EU-NORTH"),
            entry("swedencentral", "EU-NORTH"),
            entry("polandcentral", "EU-CENTRAL"),
            entry("italynorth", "EU-SOUTH"),
            entry("spaincentral", "EU-SOUTH"),
            entry("belgiumcentral", "EU-WEST"),
            entry("finlandcentral", "EU-NORTH"),

            entry("eastus", "US-EAST"),
            entry("eastus2", "US-EAST"),
            entry("westus", "US-WEST"),
            entry("westus2", "US-WEST"),
            entry("westus3", "US-WEST"),
            entry("centralus", "US-CENTRAL"),
            entry("northcentralus", "US-CENTRAL"),
            entry("southcentralus", "US-CENTRAL"),
            entry("westcentralus", "US-CENTRAL"),

            entry("brazilsouth", "SOUTH_AMERICA"),
            entry("brazilsoutheast", "SOUTH_AMERICA"),
            entry("chilecentral", "SOUTH_AMERICA"),
            entry("canadacentral", "CANADA"),
            entry("canadaeast", "CANADA"),

            entry("eastasia", "ASIA-EAST"),
            entry("southeastasia", "ASIA-SOUTHEAST"),
            entry("japaneast", "ASIA-NORTHEAST"),
            entry("japanwest", "ASIA-NORTHEAST"),
            entry("westindia", "ASIA-SOUTH"),
            entry("indonesiacentral", "ASIA-SOUTHEAST"),
            entry("malaysiawest", "ASIA-SOUTHEAST"),
            entry("koreacentral", "ASIA-NORTHEAST"),
            entry("koreasouth", "ASIA-NORTHEAST"),
            entry("centralindia", "ASIA-SOUTH"),
            entry("southindia", "ASIA-SOUTH"),
            entry("newzealandnorth", "ASIA-SOUTHEAST"),

            entry("australiacentral", "AUSTRALIA"),
            entry("australiacentral2", "AUSTRALIA"),
            entry("australiaeast", "AUSTRALIA"),
            entry("australiasoutheast", "AUSTRALIA"),

            entry("uaenorth", "MIDDLE_EAST"),
            entry("israelnorthwest", "MIDDLE_EAST"),
            entry("uaecentral", "MIDDLE_EAST"),
            entry("qatarcentral", "MIDDLE_EAST"),
            entry("israelcentral", "MIDDLE_EAST"),

            entry("southafricanorth", "AFRICA"),
            entry("southafricawest", "AFRICA")
    );

    private static final Map<String, String> AwsRegionMapping = Map.ofEntries(
            entry("us-east-1", "US-EAST"),
            entry("us-east-2", "US-EAST"),
            entry("us-west-1", "US-WEST"),
            entry("us-west-2", "US-WEST"),

            entry("ca-central-1", "CANADA"),
            entry("ca-west-1", "CANADA"),

            entry("sa-east-1", "SOUTH_AMERICA"),

            entry("eu-west-1", "EU-WEST"),
            entry("eu-west-2", "EU-WEST"),
            entry("eu-west-3", "EU-WEST"),
            entry("eu-north-1", "EU-NORTH"),
            entry("eu-central-1", "EU-CENTRAL"),
            entry("eu-central-2", "EU-CENTRAL"),
            entry("eu-south-1", "EU-SOUTH"),
            entry("eu-south-2", "EU-SOUTH"),

            entry("ap-southeast-1", "ASIA-SOUTHEAST"),
            entry("ap-southeast-3", "ASIA-SOUTHEAST"),
            entry("ap-southeast-4", "AUSTRALIA"),
            entry("ap-northeast-1", "ASIA-NORTHEAST"),
            entry("ap-northeast-2", "ASIA-NORTHEAST"),
            entry("ap-northeast-3", "ASIA-NORTHEAST"),
            entry("ap-south-1", "ASIA-SOUTH"),
            entry("ap-south-2", "ASIA-SOUTH"),
            entry("ap-east-1", "ASIA-EAST"),

            entry("ap-southeast-2", "AUSTRALIA"),

            entry("af-south-1", "AFRICA"),

            entry("me-south-1", "MIDDLE_EAST"),
            entry("me-central-1", "MIDDLE_EAST"),
            entry("il-central-1", "MIDDLE_EAST")
    );

    private static final Map<String, String> GcpRegionMapping = Map.ofEntries(
            entry("europe-west4", "EU-WEST"),
            entry("europe-west6", "EU-WEST"),
            entry("europe-west9", "EU-WEST"),
            entry("europe-west12", "EU-WEST"),
            entry("europe-north1", "EU-NORTH"),
            entry("europe-central2", "EU-CENTRAL"),


            entry("us-east5", "US-EAST"),
            entry("us-east7", "US-EAST"),
            entry("us-south1", "US-CENTRAL"),
            entry("us-west1", "US-WEST"),
            entry("us-west8", "US-WEST"),
            entry("northamerica-northeast1", "CANADA"),
            entry("northamerica-northeast2", "CANADA"),

            entry("southamerica-west1", "SOUTH_AMERICA"),

            entry("asia-east2", "ASIA-EAST"),
            entry("asia-southeast2", "ASIA-SOUTHEAST"),

            entry("australia-southeast1", "AUSTRALIA"),

            entry("me-central1", "MIDDLE_EAST")
    );

    public static String mapRegionName(String regionName, Provider provider) {
        if (regionName == null || regionName.isBlank()) {
            return "UNKNOWN";
        }

        String key = regionName.trim().toLowerCase();
        String formatted;

        if (provider == Provider.AZURE) {

            formatted = AzureRegionMapping.getOrDefault(key, "UNKNOWN");
        }else if (provider == Provider.AWS) {
            formatted = AwsRegionMapping.getOrDefault(key, "UNKNOWN");
        }else if (provider == Provider.GCP) {
            formatted = GcpRegionMapping.getOrDefault(key, "UNKNOWN");
        }else {
            formatted = "UNKNOWN";
        }

        if ("UNKNOWN".equals(formatted)) {
            UnmappedLogger.log(provider, key);
        }
        return formatted;
    }

}
