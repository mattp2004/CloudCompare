package org.cloudcompare.backend.api;

import org.cloudcompare.backend.auth.JwtService;
import org.cloudcompare.backend.db.account.AccountRepo;
import org.cloudcompare.backend.db.account.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AccountRepo repo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final JwtService jwt;

    public AuthController(AccountRepo repo, JwtService jwt) {
        this.repo = repo;
        this.jwt = jwt;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody Map<String,String> body) {

        UUID id = UUID.randomUUID();

        repo.createAccount(id, body.get("username"), body.get("email"), body.get("password"));

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("userId", id.toString(), "username", body.get("username"), "email", body.get("email")
        ));
    }

    @PostMapping("/login")
    public Map<String,String> login(@RequestBody Map<String,String> body) {

        User user = repo.retrieveByUsername(body.get("username"));

        if(user == null || !encoder.matches(body.get("password"), user.hashedPassword)){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        String token = jwt.generateToken(user.getId(), user.getUsername());
        return Map.of("token", token);
    }
}
