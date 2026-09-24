package vn.gov.tax.organization.controller;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.gov.tax.common.response.ApiResponse;
import vn.gov.tax.organization.client.ProvinceClient;
import vn.gov.tax.organization.client.ProvinceResponse;
import vn.gov.tax.organization.entity.Organization;
import vn.gov.tax.organization.repository.OrganizationRepository;
@RestController @RequestMapping("/api/organizations") public class OrganizationController {
 private final OrganizationRepository repository; private final ProvinceClient provinceClient;
 public OrganizationController(OrganizationRepository repository, ProvinceClient provinceClient){this.repository=repository;this.provinceClient=provinceClient;}
 @GetMapping("/province/{id}") public ApiResponse<ProvinceResponse> findProvince(@PathVariable Long id){return provinceClient.findById(id);}
 @GetMapping public ApiResponse<List<Organization>> findAll(){return ApiResponse.success(repository.findAll());}
 @GetMapping("/{id}") public ApiResponse<Organization> findById(@PathVariable Long id){return ApiResponse.success(repository.findById(id).orElseThrow());}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public ApiResponse<Organization> create(@RequestBody Organization value){return ApiResponse.success(repository.save(value));}
 @PutMapping("/{id}") public ApiResponse<Organization> update(@PathVariable Long id,@RequestBody Organization input){Organization v=repository.findById(id).orElseThrow();v.setCode(input.getCode());v.setName(input.getName());v.setProvinceCode(input.getProvinceCode());v.setType(input.getType());return ApiResponse.success(repository.save(v));}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id){repository.deleteById(id);}
}
