package org.cloudcompare.backend.api.admin;

import org.cloudcompare.backend.db.offers.ServiceOfferRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class DbAdminController {
    ServiceOfferRepo serviceOfferRepo;
    public DbAdminController(ServiceOfferRepo repo){
        serviceOfferRepo = repo;
    }

    @GetMapping("/wipe")
    public ResponseEntity<?> wipe(){
        serviceOfferRepo.clearOffers();
        return ResponseEntity.ok().build();
    }
}
