package org.cloudcompare.backend.api;

import org.cloudcompare.backend.catalog.Price;
import org.cloudcompare.backend.db.offers.PriceRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/prices")
public class PriceController {
    private final PriceRepo priceRepo;

    public PriceController(PriceRepo priceRepo) {
        this.priceRepo = priceRepo;
    }

    @GetMapping("/{offerId}")
    public ResponseEntity<Price> getPriceByOfferId(@PathVariable String offerId){
        Price price = priceRepo.retrieveByOffer(offerId);
        if(price == null){ return ResponseEntity.notFound().build();}
        return ResponseEntity.ok(price);
    }
}