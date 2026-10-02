package vn.gov.tax.dataplatform.dataset;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.gov.tax.common.response.ApiResponse;
import vn.gov.tax.dataplatform.response.PageResponse;
import vn.gov.tax.dataplatform.security.TenantContext;

@RestController
@RequestMapping("/api/v1/datasets")
@RequiredArgsConstructor
public class DatasetController {
  private final DatasetRepository repo;

  @GetMapping
  @PreAuthorize("hasAnyRole('tax-officer','supervisor')")
  public ApiResponse<PageResponse<Dataset>> list(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.success(
        PageResponse.from(
            repo.findAllByTenantId(
                TenantContext.require(),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)))));
  }
}
