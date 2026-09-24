package vn.gov.tax.taxpayer.controller;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.gov.tax.common.response.ApiResponse;
import vn.gov.tax.taxpayer.entity.Taxpayer;
import vn.gov.tax.taxpayer.repository.TaxpayerRepository;
@RestController @RequestMapping("/api/taxpayers") public class TaxpayerController {
 private final TaxpayerRepository repository; public TaxpayerController(TaxpayerRepository repository){this.repository=repository;}
 @GetMapping public ApiResponse<List<Taxpayer>> findAll(){return ApiResponse.success(repository.findAll());}
 @GetMapping("/{id}") public ApiResponse<Taxpayer> findById(@PathVariable Long id){return ApiResponse.success(repository.findById(id).orElseThrow());}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public ApiResponse<Taxpayer> create(@RequestBody Taxpayer value){return ApiResponse.success(repository.save(value));}
 @PutMapping("/{id}") public ApiResponse<Taxpayer> update(@PathVariable Long id,@RequestBody Taxpayer input){Taxpayer v=repository.findById(id).orElseThrow();v.setTaxCode(input.getTaxCode());v.setName(input.getName());v.setAddress(input.getAddress());v.setProvinceCode(input.getProvinceCode());v.setPhone(input.getPhone());return ApiResponse.success(repository.save(v));}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id){repository.deleteById(id);}
}
