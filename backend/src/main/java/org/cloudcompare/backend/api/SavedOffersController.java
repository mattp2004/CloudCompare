package org.cloudcompare.backend.api;

import org.cloudcompare.backend.auth.JwtUser;
import org.cloudcompare.backend.db.account.SavedOfferRepo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/saved")
public class SavedOffersController {

    private final SavedOfferRepo repo;

    public SavedOffersController(SavedOfferRepo repo) {
        this.repo = repo;
    }

    @PostMapping("/{offerId}")
    public void save(@PathVariable String offerId, @AuthenticationPrincipal JwtUser principal) {
        repo.save(principal.userId(), offerId);
    }

    @GetMapping("/debug")
    public Object debug(@AuthenticationPrincipal JwtUser principal) {
        return principal;
    }

    @GetMapping
    public List<?> list(@AuthenticationPrincipal JwtUser principal) {
        return repo.list(principal.userId());
    }

    @DeleteMapping("/{offerId}")
    public void delete(@PathVariable String offerId, @AuthenticationPrincipal JwtUser principal) {
        repo.delete(principal.userId(), offerId);
    }
}
