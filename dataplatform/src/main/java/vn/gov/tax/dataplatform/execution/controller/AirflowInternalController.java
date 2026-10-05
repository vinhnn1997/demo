package vn.gov.tax.dataplatform.execution.controller;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.gov.tax.common.response.ApiResponse;
import vn.gov.tax.dataplatform.execution.service.ExecutionService;
import vn.gov.tax.dataplatform.source.dto.DatabaseConnectionRequest;

@RestController
@RequestMapping("/api/v1/internal/executions")
@RequiredArgsConstructor
public class AirflowInternalController {
  private final ExecutionService service;

  @GetMapping("/{executionId}/source-connection")
  @PreAuthorize("hasRole('data-platform-worker')")
  public ApiResponse<DatabaseConnectionRequest> sourceConnection(@PathVariable UUID executionId) {
    return ApiResponse.success(service.connectionForWorker(executionId));
  }
}