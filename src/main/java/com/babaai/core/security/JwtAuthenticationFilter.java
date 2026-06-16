package com.babaai.core.security;

import com.babaai.core.domain.User;
import com.babaai.core.repository.UserRepository;
import com.babaai.core.service.PresenceService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PermissionResolver permissionResolver;
    private final PresenceService presenceService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository,
            PermissionResolver permissionResolver,
            PresenceService presenceService
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.permissionResolver = permissionResolver;
        this.presenceService = presenceService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            Optional<UUID> userId = jwtService.parseUserId(token);
            if (userId.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
                Optional<User> userOpt = userRepository.findWithRolesAndPermissionsById(userId.get());
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    // Account kill-switch: a disabled account is rejected immediately with an explicit
                    // reason the FE can show. The filter reloads the user every request, so disabling
                    // takes effect at once on core endpoints (no token-expiry wait). 403 (not 401) so
                    // the client surfaces the reason instead of trying to refresh.
                    if (!user.isEnabled()) {
                        response.setStatus(HttpStatus.FORBIDDEN.value());
                        response.setContentType("application/json");
                        response.getWriter().write("{\"detail\":\"Account is disabled\"}");
                        return;
                    }
                    List<SimpleGrantedAuthority> authorities = permissionResolver.effectivePermissions(user).stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList();
                    AuthenticatedUser principal = new AuthenticatedUser(user, authorities);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // Presence — at most one write per throttle window.
                    Instant now = Instant.now();
                    if (presenceService.isStale(user.getLastSeenAt(), now)) {
                        presenceService.touch(user.getId(), now);
                    }
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
