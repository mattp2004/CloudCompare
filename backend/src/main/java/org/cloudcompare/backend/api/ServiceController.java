package org.cloudcompare.backend.api;

import org.cloudcompare.backend.catalog.Service;
import org.cloudcompare.backend.catalog.ServiceOffer;
import org.cloudcompare.backend.db.offers.ServiceOfferRepo;
import org.cloudcompare.backend.db.ServiceRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/services")
public class ServiceController {
    private final ServiceRepo serviceRepo;
    private final ServiceOfferRepo serviceOfferRepo;

    public ServiceController(ServiceRepo serviceRepo, ServiceOfferRepo serviceOfferRepo) {
        this.serviceRepo = serviceRepo;
        this.serviceOfferRepo = serviceOfferRepo;
    }

    // GET /api/services
    @GetMapping()
    public List<Service> getAllServices(){
        return serviceRepo.retrieveAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Service> getServiceById(@PathVariable String id){
        Service service = serviceRepo.retrieveById(id);
        if(service == null){ return ResponseEntity.notFound().build();}
        return ResponseEntity.ok(service);
    }

    @GetMapping("/{id}/offers")
    public List<ServiceOffer> getOffersForServiceID(@PathVariable String id){
        return serviceOfferRepo.retrieveByServiceID(id);
    }
}