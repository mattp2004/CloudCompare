package org.cloudcompare.backend.api;

import org.cloudcompare.backend.catalog.ResultsPage;
import org.cloudcompare.backend.db.offers.ServiceOfferRepo;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/offers")
public class OffersController {

    private final ServiceOfferRepo serviceOfferRepo;

    public OffersController(ServiceOfferRepo serviceOfferRepo) {
        this.serviceOfferRepo = serviceOfferRepo;
    }

    @GetMapping(produces = "application/json")
    public ResultsPage list(
            @RequestParam(required = false) String serviceId,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String orderBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return serviceOfferRepo.searchForOffers(serviceId, provider,region,type,unit,q,orderBy,page,size);
    }
}
