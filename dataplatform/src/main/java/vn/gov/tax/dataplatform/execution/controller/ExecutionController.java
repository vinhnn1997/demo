package vn.gov.tax.dataplatform.execution.controller;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.gov.tax.common.response.ApiResponse;
import vn.gov.tax.dataplatform.execution.dto.ExecutionResponse;
import vn.gov.tax.dataplatform.execution.service.ExecutionService;
import vn.gov.tax.dataplatform.response.PageResponse;
import vn.gov.tax.dataplatform.security.TenantContext;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ExecutionController {
  private final ExecutionService service;

  @PostMapping("/pipelines/{pipelineId}/runs")
  @ResponseStatus(HttpStatus.ACCEPTED)
  @PreAuthorize("hasRole('supervisor')")
  public ApiResponse<ExecutionResponse> trigger(@PathVariable UUID pipelineId) {
    return ApiResponse.success(service.trigger(TenantContext.require(), pipelineId));
  }

  @GetMapping("/pipelines/{pipelineId}/runs")
  @PreAuthorize("hasAnyRole('tax-officer','supervisor')")
  public ApiResponse<PageResponse<ExecutionResponse>> list(
      @PathVariable UUID pipelineId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.success(PageResponse.from(service.list(
        TenantContext.require(), pipelineId,
        PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)))));
  }

  @GetMapping("/executions/{id}")
  @PreAuthorize("hasAnyRole('tax-officer','supervisor')")
  public ApiResponse<ExecutionResponse> get(@PathVariable UUID id) {
    return ApiResponse.success(service.get(TenantContext.require(), id));
  }
}