package vn.gov.tax.identity.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.gov.tax.common.response.ApiResponse;
import vn.gov.tax.identity.dto.KeycloakUserResponse;
import vn.gov.tax.identity.dto.UpdateUserRolesRequest;
import vn.gov.tax.identity.service.IdentityAdminService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminUserController {
    private final IdentityAdminService identityAdminService;

    @GetMapping("/users")
    public ApiResponse<List<KeycloakUserResponse>> searchUsers(@RequestParam(required = false) String search) {
        return ApiResponse.success(identityAdminService.searchUsers(search));
    }

    @GetMapping("/roles")
    public ApiResponse<List<String>> listManagedRoles() {
        return ApiResponse.success(identityAdminService.listManagedRoles());
    }

    @GetMapping("/users/{userId}/roles")
    public ApiResponse<List<String>> getUserRoles(@PathVariable String userId) {
        return ApiResponse.success(identityAdminService.getUserManagedRoles(userId));
    }

    @PutMapping("/users/{userId}/roles")
    public ApiResponse<List<String>> replaceUserRoles(
            @PathVariable String userId, @RequestBody UpdateUserRolesRequest request) {
        return ApiResponse.success(identityAdminService.replaceUserManagedRoles(
                userId, request == null ? null : request.roles()));
    }
}
