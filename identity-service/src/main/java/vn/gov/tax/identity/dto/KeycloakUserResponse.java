package vn.gov.tax.identity.dto;

public record KeycloakUserResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        Boolean enabled) {
}
