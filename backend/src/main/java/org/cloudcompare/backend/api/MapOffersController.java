package org.cloudcompare.backend.api;

import org.cloudcompare.backend.catalog.MapResults;
import org.cloudcompare.backend.db.offers.ServiceOfferRepo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/offers/map")
public class MapOffersController {

    private final ServiceOfferRepo serviceOfferRepo;

    public MapOffersController(ServiceOfferRepo serviceOfferRepo) {
        this.serviceOfferRepo = serviceOfferRepo;
    }

    @GetMapping(produces = "application/json")
    public List<MapResults> map(
            @RequestParam(required = false) String serviceId,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String type
    ) {
        return serviceOfferRepo.searchForMap(
                serviceId,
                provider,
                region,
                type
        );
    }
}
