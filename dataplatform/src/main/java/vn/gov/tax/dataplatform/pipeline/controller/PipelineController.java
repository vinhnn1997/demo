package vn.gov.tax.dataplatform.pipeline.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.gov.tax.common.response.ApiResponse;
import vn.gov.tax.dataplatform.compiler.model.PlanValidationResult;
import vn.gov.tax.dataplatform.pipeline.dto.PipelineRequest;
import vn.gov.tax.dataplatform.pipeline.dto.PipelineResponse;
import vn.gov.tax.dataplatform.pipeline.service.PipelineService;
import vn.gov.tax.dataplatform.response.PageResponse;
import vn.gov.tax.dataplatform.security.TenantContext;

@RestController
@RequestMapping("/api/v1/pipelines")
@RequiredArgsConstructor
public class PipelineController {
  private final PipelineService service;

  @GetMapping
  @PreAuthorize("hasAnyRole('tax-officer','supervisor')")
  public ApiResponse<PageResponse<PipelineResponse>> list(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.success(PageResponse.from(service.list(
        TenantContext.require(), PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)))));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('tax-officer','supervisor')")
  public ApiResponse<PipelineResponse> get(@PathVariable UUID id) {
    return ApiResponse.success(service.get(TenantContext.require(), id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('supervisor')")
  public ApiResponse<PipelineResponse> create(@Valid @RequestBody PipelineRequest request) {
    return ApiResponse.success(service.create(TenantContext.require(), request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('supervisor')")
  public ApiResponse<PipelineResponse> update(
      @PathVariable UUID id, @Valid @RequestBody PipelineRequest request) {
    return ApiResponse.success(service.update(TenantContext.require(), id, request));
  }

  @PostMapping("/{id}/compile")
  @PreAuthorize("hasAnyRole('tax-officer','supervisor')")
  public ApiResponse<PlanValidationResult> compile(@PathVariable UUID id) {
    return ApiResponse.success(service.compile(TenantContext.require(), id));
  }
}