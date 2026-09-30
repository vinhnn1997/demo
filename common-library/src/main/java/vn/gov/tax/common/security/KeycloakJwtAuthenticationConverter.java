package vn.gov.tax.common.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import lombok.RequiredArgsConstructor;

public final class KeycloakJwtAuthenticationConverter {
    private KeycloakJwtAuthenticationConverter() {
    }

    public static JwtAuthenticationConverter create(String clientId) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter(clientId));
        return converter;
    }

    @RequiredArgsConstructor
    private static final class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        private final String clientId;

        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Set<String> roles = new LinkedHashSet<>();
            addRoles(jwt.getClaimAsMap("realm_access"), roles);

            Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
            if (resourceAccess != null) {
                addRoles(resourceAccess.get(clientId), roles);
            }

            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toSet());
        }

        private static void addRoles(Object access, Set<String> roles) {
            if (access instanceof Map<?, ?> accessMap
                    && accessMap.get("roles") instanceof Collection<?> values) {
                values.stream()
                        .map(Object::toString)
                        .map(KeycloakRoleConverter::removeRolePrefix)
                        .forEach(roles::add);
            }
        }

        private static String removeRolePrefix(String role) {
            return role.startsWith("ROLE_") ? role.substring("ROLE_".length()) : role;
        }
    }
}
