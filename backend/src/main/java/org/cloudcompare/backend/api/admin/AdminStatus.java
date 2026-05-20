package org.cloudcompare.backend.api.admin;

import org.cloudcompare.backend.db.offers.ServiceOfferRepo;
import org.cloudcompare.backend.ingest.IngestionManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/status")
public class AdminStatus {

    IngestionManager ingestionManager;
    public AdminStatus(IngestionManager ingestionManager){
        this.ingestionManager = ingestionManager;
    }

    @GetMapping(produces = "application/json")
    public Map<String, Object> status() {
        return ingestionManager.getStatus();
    }
}
