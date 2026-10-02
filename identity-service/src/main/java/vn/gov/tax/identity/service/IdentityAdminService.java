package vn.gov.tax.identity.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import vn.gov.tax.common.security.TokenRevocationService;
import vn.gov.tax.identity.client.KeycloakAdminClient;
import vn.gov.tax.identity.client.KeycloakAdminClient.RoleRepresentation;
import vn.gov.tax.identity.config.KeycloakAdminProperties;
import vn.gov.tax.identity.dto.KeycloakUserResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IdentityAdminService {
    private final KeycloakAdminClient keycloakAdminClient;
    private final KeycloakAdminProperties properties;
    private final TokenRevocationService tokenRevocationService;

    public List<KeycloakUserResponse> searchUsers(String search) {
        List<KeycloakUserResponse> users = keycloakAdminClient.searchUsers(search);
        return users == null ? List.of() : users;
    }

    public List<String> listManagedRoles() {
        return properties.getManagedRoles();
    }

    public List<String> getUserManagedRoles(String userId) {
        requireUser(userId);
        return currentManagedRoleNames(userId);
    }

    public List<String> replaceUserManagedRoles(String userId, List<String> requestedRoles) {
        requireUser(userId);
        List<String> desired = normalizeRequestedRoles(requestedRoles);

        tokenRevocationService.revokeTokensIssuedNotAfter(userId, Instant.now());

        Set<String> current = new LinkedHashSet<>(currentManagedRoleNames(userId));
        List<String> toRemove = current.stream().filter(role -> !desired.contains(role)).toList();
        List<String> toAdd = desired.stream().filter(role -> !current.contains(role)).toList();

        keycloakAdminClient.removeRealmRoles(userId, resolveRoles(toRemove));
        keycloakAdminClient.addRealmRoles(userId, resolveRoles(toAdd));

        tokenRevocationService.revokeTokensIssuedNotAfter(userId, Instant.now());
        return currentManagedRoleNames(userId);
    }

    private void requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        if (keycloakAdminClient.getUser(userId) == null) {
            throw new NoSuchElementException("User not found");
        }
    }

    private List<String> currentManagedRoleNames(String userId) {
        Set<String> managed = new LinkedHashSet<>(properties.getManagedRoles());
        return keycloakAdminClient.getRealmRolesForUser(userId).stream()
                .map(RoleRepresentation::name)
                .filter(Objects::nonNull)
                .filter(managed::contains)
                .sorted()
                .toList();
    }

    private List<String> normalizeRequestedRoles(List<String> requestedRoles) {
        if (requestedRoles == null) {
            throw new IllegalArgumentException("roles is required");
        }
        Set<String> managed = new LinkedHashSet<>(properties.getManagedRoles());
        Set<String> desired = new LinkedHashSet<>();
        for (String role : requestedRoles) {
            if (role == null || role.isBlank()) {
                continue;
            }
            String normalized = role.trim();
            if (!managed.contains(normalized)) {
                throw new IllegalArgumentException("Role is not managed by this API: " + normalized);
            }
            desired.add(normalized);
        }
        return List.copyOf(desired);
    }

    private List<RoleRepresentation> resolveRoles(List<String> roleNames) {
        List<RoleRepresentation> resolved = new ArrayList<>();
        for (String roleName : roleNames) {
            RoleRepresentation role = keycloakAdminClient.getRealmRole(roleName);
            if (role == null || role.id() == null) {
                throw new IllegalArgumentException("Keycloak realm role not found: " + roleName);
            }
            resolved.add(role);
        }
        return resolved;
    }
}
