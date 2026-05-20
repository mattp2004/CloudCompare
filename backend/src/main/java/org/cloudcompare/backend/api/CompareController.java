package org.cloudcompare.backend.api;

import org.cloudcompare.backend.catalog.Compare;
import org.cloudcompare.backend.db.offers.CompareRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/compare")
public class CompareController {

    private final CompareRepo compareRepo;

    public CompareController(CompareRepo compareRepo) {
        this.compareRepo = compareRepo;
    }

    @GetMapping("/{offerId}")
    public ResponseEntity<Compare> compare(@PathVariable String offerId,@RequestParam(defaultValue = "50") int limit) {
        Compare comparison = compareRepo.makeComparisonById(offerId, limit);
        if (comparison== null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(comparison);
    }

    @GetMapping("/ai/{offerId}")
    public ResponseEntity<Map<String,Object>> compareAi(@PathVariable String offerId, @RequestParam() String offer2Id)
    {
        Map<String,Object> comparison = compareRepo.aiComparison(offerId, offer2Id);
        if (comparison == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(comparison);
    }
}
