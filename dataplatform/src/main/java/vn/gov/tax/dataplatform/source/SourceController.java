package vn.gov.tax.dataplatform.source;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.gov.tax.common.response.ApiResponse;
import vn.gov.tax.dataplatform.response.PageResponse;
import vn.gov.tax.dataplatform.security.TenantContext;
import vn.gov.tax.dataplatform.source.connector.DatabaseConnectionTester;

@RestController
@RequestMapping("/api/v1/sources")
@RequiredArgsConstructor
public class SourceController {
  private final SourceService service;

  @GetMapping
  @PreAuthorize("hasAnyRole('tax-officer','supervisor')")
  public ApiResponse<PageResponse<SourceResponse>> list(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.success(
        PageResponse.from(
            service.list(
                requireTenant(),
                PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 100)))));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('tax-officer','supervisor')")
  public ApiResponse<SourceResponse> get(@PathVariable UUID id) {
    return ApiResponse.success(service.get(requireTenant(), id));
  }

  @PostMapping("/connection-test")
  @PreAuthorize("hasRole('supervisor')")
    public ApiResponse<DatabaseConnectionTester.ConnectionTestResult> testConnection(
      @Valid @RequestBody ConnectionTestRequest request) {
    requireTenant();
    return ApiResponse.success(service.testConnection(request));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('supervisor')")
  public ApiResponse<SourceResponse> create(@Valid @RequestBody SourceRequest request) {
    return ApiResponse.success(service.create(requireTenant(), request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('supervisor')")
  public ApiResponse<SourceResponse> update(
      @PathVariable UUID id, @Valid @RequestBody SourceRequest request) {
    return ApiResponse.success(service.update(requireTenant(), id, request));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('supervisor')")
  public void delete(@PathVariable UUID id) {
    service.delete(requireTenant(), id);
  }

  private String requireTenant() {
    return TenantContext.require();
  }
}
