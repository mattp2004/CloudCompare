package org.cloudcompare.backend.api.admin;

import org.cloudcompare.backend.ingest.clients.AwsIngestionClient;
import org.cloudcompare.backend.ingest.clients.AzureIngestionClient;
import org.cloudcompare.backend.ingest.clients.GcpIngestionClient;
import org.cloudcompare.backend.ingest.Ingestion;
import org.cloudcompare.backend.util.Logger;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/ingest")
public class IngestController {

    private final AzureIngestionClient azureIngestClient;
    private final AwsIngestionClient awsIngestClient;
    private final GcpIngestionClient gcpIngestClient;

    public IngestController(AzureIngestionClient azureIngestClient, AwsIngestionClient awsIngestClient, GcpIngestionClient gcpIngestClient) {
        this.azureIngestClient = azureIngestClient;
        this.awsIngestClient = awsIngestClient;
        this.gcpIngestClient = gcpIngestClient;
    }

    @GetMapping("/azure")
    public Ingestion ingestAzure() {
        return azureIngestClient.ingestAll();
    }

    @GetMapping("/aws")
    public Ingestion ingestAws() {
        Logger.Log("Starting AWS ingestion");
        return awsIngestClient.ingestEc2();
    }
    @GetMapping("/aws/rds")
    public Ingestion ingestAwsRds() {
        Logger.Log("Starting AWS RDS ingestion");
        return awsIngestClient.ingestRds();
    }

    @GetMapping("/aws/s3")
    public Ingestion ingestAwsS3() {
        Logger.Log("Starting AWS S3 ingestion");
        return awsIngestClient.ingestS3();
    }

    @GetMapping("/aws/ec2")
    public Ingestion ingestAwsEc2() {
        Logger.Log("Starting AWS EC2 ingestion");
        return awsIngestClient.ingestEc2();
    }

    @GetMapping("/gcp")
    public Ingestion ingestGcp(){
        Logger.Log("HIT /admin/ingest/gcp - start");
        Ingestion res = gcpIngestClient.ingestAll();
        Logger.Log("HIT /admin/ingest/gcp - end offers=" + res.offersStored + " prices=" + res.pricesStored);
        return res;
    }

}
