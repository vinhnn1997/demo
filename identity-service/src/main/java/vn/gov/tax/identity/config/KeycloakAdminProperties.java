package vn.gov.tax.identity.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.keycloak")
public class KeycloakAdminProperties {
    private String adminUrl = "http://localhost:8080";
    private String adminClientId = "tax-admin-api";
    private String adminClientSecret;
    private String realm = "tax-platform";
    private List<String> managedRoles = List.of("tax-officer", "supervisor");

    public String getAdminUrl() {
        return adminUrl;
    }

    public void setAdminUrl(String adminUrl) {
        this.adminUrl = adminUrl;
    }

    public String getAdminClientId() {
        return adminClientId;
    }

    public void setAdminClientId(String adminClientId) {
        this.adminClientId = adminClientId;
    }

    public String getAdminClientSecret() {
        return adminClientSecret;
    }

    public void setAdminClientSecret(String adminClientSecret) {
        this.adminClientSecret = adminClientSecret;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public List<String> getManagedRoles() {
        return managedRoles;
    }

    public void setManagedRoles(List<String> managedRoles) {
        Set<String> unique = new LinkedHashSet<>();
        if (managedRoles != null) {
            for (String role : managedRoles) {
                if (role != null && !role.isBlank()) {
                    unique.add(role.trim());
                }
            }
        }
        this.managedRoles = List.copyOf(new ArrayList<>(unique));
    }
}
