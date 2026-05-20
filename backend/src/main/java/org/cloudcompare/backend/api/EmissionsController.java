package org.cloudcompare.backend.api;

import org.cloudcompare.backend.co2.EmissionManager;
import org.cloudcompare.backend.co2.catalog.Emissions;
import org.cloudcompare.backend.co2.catalog.EmissionsMap;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/emissions")
public class EmissionsController {

    private EmissionManager emissionManager;

    public EmissionsController(EmissionManager emissionManager){
        this.emissionManager = emissionManager;
    }

    @GetMapping("/{offerId}")
    public ResponseEntity<Emissions> getEmissionsForOffer(@PathVariable String offerId){
        Emissions emission = emissionManager.calculateForOffer(offerId);
        if(emission != null){
            return ResponseEntity.ok(emission);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/compare/{offerId}")
    public ResponseEntity<List<Emissions>> getForComparison(@PathVariable String offerId, @RequestParam(defaultValue="50") int limit){
        List<Emissions> results = emissionManager.calculateForComparison(offerId, limit);
        if(results.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(results);
    }

    @GetMapping("/map")
    public List<EmissionsMap> getEmissionsForMap(@RequestParam(required=false) String serviceId, @RequestParam(required = false) String provider, @RequestParam(required = false) String region, @RequestParam(required=false) String type){
        return emissionManager.calculateForMap(serviceId, provider, region,type);
    }
}
