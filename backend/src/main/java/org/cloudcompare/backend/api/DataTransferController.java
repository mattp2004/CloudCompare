package org.cloudcompare.backend.api;

import org.cloudcompare.backend.catalog.DataTransfer;
import org.cloudcompare.backend.catalog.MapResults;
import org.cloudcompare.backend.db.DataTransferRepo;
import org.cloudcompare.backend.db.offers.ServiceOfferRepo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/offers/dt")
public class DataTransferController {

    private final DataTransferRepo dataTransferRepo;

    public DataTransferController(DataTransferRepo dataTransferRepo) {
        this.dataTransferRepo = dataTransferRepo;
    }

    @GetMapping(produces = "application/json")
    public List<DataTransfer> dt(
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String serviceId
    ) {
        return dataTransferRepo.retrieveDataTransfer(
                provider,
                type,
                serviceId
        );
    }
}