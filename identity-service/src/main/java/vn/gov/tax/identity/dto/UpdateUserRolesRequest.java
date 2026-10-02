package vn.gov.tax.identity.dto;

import java.util.List;

public record UpdateUserRolesRequest(List<String> roles) {
}
