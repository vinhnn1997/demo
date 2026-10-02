package vn.gov.tax.dataplatform.example;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.gov.tax.common.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/examples")
public class ExampleController {
  private static final DashboardExample DASHBOARD =
      new DashboardExample(12, 8, "OPERATIONAL");

  private static final List<SourceExample> SOURCES =
      List.of(
          new SourceExample(
              "sample-postgres-orders",
              "Kho dữ liệu đơn hàng",
              "POSTGRES",
              "Nguồn PostgreSQL mẫu cho dữ liệu đơn hàng.",
              "ACTIVE"),
          new SourceExample(
              "sample-mysql-customers",
              "Hệ thống khách hàng",
              "MYSQL",
              "Nguồn MySQL mẫu cho hồ sơ khách hàng.",
              "ACTIVE"),
          new SourceExample(
              "sample-rest-inventory",
              "API tồn kho",
              "REST_API",
              "Nguồn REST mẫu cho dữ liệu tồn kho.",
              "DISABLED"),
          new SourceExample(
              "sample-postgres-finance",
              "Kho dữ liệu tài chính",
              "POSTGRES",
              "Nguồn PostgreSQL mẫu cho báo cáo tài chính.",
              "ACTIVE"));

  @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('tax-officer','supervisor')")
  public ApiResponse<DashboardExample> dashboard() {
        return ApiResponse.success(DASHBOARD);
  }

  @GetMapping("/sources")
    @PreAuthorize("hasAnyRole('tax-officer','supervisor')")
  public ApiResponse<List<SourceExample>> sources() {
        return ApiResponse.success(SOURCES);
  }

  public record DashboardExample(int totalSources, int totalDatasets, String apiStatus) {}

  public record SourceExample(
      String id, String name, String type, String description, String status) {}
}
