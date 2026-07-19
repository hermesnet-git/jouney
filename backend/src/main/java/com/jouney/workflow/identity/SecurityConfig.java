package com.jouney.workflow.identity;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * T007/T008 — autenticação OpenID Connect (resource server) + autorização por papel (FR-022).
 * Endpoints usam @PreAuthorize("hasRole('...')") com os valores de {@link Role}; este config só
 * traduz o claim "roles" do token JWT em GrantedAuthority "ROLE_<papel>".
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .oauth2ResourceServer(
            oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())));
    return http.build();
  }

  private JwtAuthenticationConverter jwtAuthConverter() {
    JwtGrantedAuthoritiesConverter scopesConverter = new JwtGrantedAuthoritiesConverter();
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(
        jwt -> {
          List<GrantedAuthority> fromScopes = (List<GrantedAuthority>) scopesConverter.convert(jwt);
          List<GrantedAuthority> fromRoles = rolesFromClaim(jwt);
          return java.util.stream.Stream.concat(fromScopes.stream(), fromRoles.stream())
              .collect(Collectors.toList());
        });
    return converter;
  }

  @SuppressWarnings("unchecked")
  private List<GrantedAuthority> rolesFromClaim(Jwt jwt) {
    List<String> roles = jwt.getClaimAsStringList("roles");
    if (roles == null) {
      return List.of();
    }
    return roles.stream()
        .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
        .collect(Collectors.toList());
  }
}
