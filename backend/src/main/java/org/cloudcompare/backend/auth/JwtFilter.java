package org.cloudcompare.backend.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.cloudcompare.backend.db.account.AccountRepo;
import org.cloudcompare.backend.db.account.User;
import org.cloudcompare.backend.db.util.Rank;
import org.cloudcompare.backend.util.Logger;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final AccountRepo accountRepo;

    public JwtFilter(JwtService jwtService, UserDetailsService uds, AccountRepo accountRepo) {
        this.jwtService = jwtService;
        this.userDetailsService = uds;
        this.accountRepo = accountRepo;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain
    ) throws ServletException, IOException {
        String authentication = request.getHeader("Authorization");

        if (authentication == null || !authentication.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authentication.substring(7);
        try {
            String username = jwtService.extractUsername(token);
            UUID userId = jwtService.extractUserId(token);

            if (username != null && userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails user = userDetailsService.loadUserByUsername(username);
                if (jwtService.isTokenValid(token, user)) {
                    User t_user = accountRepo.retrieveByUsername(username);
                    Rank rank = Rank.Default;
                    if(t_user != null){rank = t_user.rank;
                    }
                    JwtUser jwtUser = new JwtUser(userId,user,rank);

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(jwtUser, null, jwtUser.getAuthorities());

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

        } catch (Exception e) {
            Logger.LogError(e.toString());
            e.printStackTrace();
        }

        chain.doFilter(request, response);
    }
    
}
