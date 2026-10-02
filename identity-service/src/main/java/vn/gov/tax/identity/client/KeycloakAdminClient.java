package vn.gov.tax.identity.client;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import vn.gov.tax.identity.config.KeycloakAdminProperties;
import vn.gov.tax.identity.dto.KeycloakUserResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KeycloakAdminClient {
    private final KeycloakAdminProperties properties;
    private final RestClient.Builder restClientBuilder;
    private final AtomicReference<CachedToken> cachedToken = new AtomicReference<>();

    public List<KeycloakUserResponse> searchUsers(String search) {
        KeycloakUserResponse[] users = adminClient().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/realms/{realm}/users")
                        .queryParam("search", search == null ? "" : search)
                        .queryParam("max", 50)
                        .build(properties.getRealm()))
                .retrieve()
                .body(KeycloakUserResponse[].class);
        return users == null ? List.of() : Arrays.asList(users);
    }

    public KeycloakUserResponse getUser(String userId) {
        try {
            return adminClient().get()
                    .uri("/admin/realms/{realm}/users/{userId}", properties.getRealm(), userId)
                    .retrieve()
                    .body(KeycloakUserResponse.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return null;
            }
            throw new IllegalStateException("Failed to load Keycloak user " + userId, ex);
        }
    }

    public List<RoleRepresentation> getRealmRolesForUser(String userId) {
        RoleRepresentation[] roles = adminClient().get()
                .uri("/admin/realms/{realm}/users/{userId}/role-mappings/realm", properties.getRealm(), userId)
                .retrieve()
                .body(RoleRepresentation[].class);
        return roles == null ? List.of() : Arrays.asList(roles);
    }

    public RoleRepresentation getRealmRole(String roleName) {
        try {
            return adminClient().get()
                    .uri("/admin/realms/{realm}/roles/{roleName}", properties.getRealm(), roleName)
                    .retrieve()
                    .body(RoleRepresentation.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return null;
            }
            throw new IllegalStateException("Failed to load Keycloak role " + roleName, ex);
        }
    }

    public void addRealmRoles(String userId, List<RoleRepresentation> roles) {
        if (roles == null || roles.isEmpty()) {
            return;
        }
        adminClient().post()
                .uri("/admin/realms/{realm}/users/{userId}/role-mappings/realm", properties.getRealm(), userId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(roles)
                .retrieve()
                .toBodilessEntity();
    }

    public void removeRealmRoles(String userId, List<RoleRepresentation> roles) {
        if (roles == null || roles.isEmpty()) {
            return;
        }
        adminClient().method(org.springframework.http.HttpMethod.DELETE)
                .uri("/admin/realms/{realm}/users/{userId}/role-mappings/realm", properties.getRealm(), userId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(roles)
                .retrieve()
                .toBodilessEntity();
    }

    private RestClient adminClient() {
        return restClientBuilder
                .baseUrl(trimTrailingSlash(properties.getAdminUrl()))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
                .build();
    }

    private String accessToken() {
        CachedToken current = cachedToken.get();
        if (current != null && current.expiresAt().isAfter(Instant.now().plusSeconds(30))) {
            return current.token();
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.getAdminClientId());
        form.add("client_secret", properties.getAdminClientSecret());

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClientBuilder
                    .baseUrl(trimTrailingSlash(properties.getAdminUrl()))
                    .build()
                    .post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", properties.getRealm())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
            if (response == null || response.get("access_token") == null) {
                throw new IllegalStateException("Keycloak admin token response was empty");
            }
            String token = response.get("access_token").toString();
            long expiresIn = response.get("expires_in") instanceof Number number
                    ? number.longValue()
                    : 60L;
            cachedToken.set(new CachedToken(token, Instant.now().plusSeconds(Math.max(30L, expiresIn))));
            return token;
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("Failed to obtain Keycloak admin access token", ex);
        }
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    public record RoleRepresentation(String id, String name) {
    }

    private record CachedToken(String token, Instant expiresAt) {
    }
}
