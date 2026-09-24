package vn.gov.tax.organization.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import vn.gov.tax.common.client.FeignCommonConfig;
import vn.gov.tax.common.response.ApiResponse;

@FeignClient(name = "province-service", configuration = FeignCommonConfig.class)
public interface ProvinceClient {
    @GetMapping("/api/provinces/internal/{id}")
    ApiResponse<ProvinceResponse> findById(@PathVariable("id") Long id);
}