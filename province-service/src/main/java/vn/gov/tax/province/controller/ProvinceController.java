package vn.gov.tax.province.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.gov.tax.common.response.ApiResponse;
import vn.gov.tax.province.entity.Province;
import vn.gov.tax.province.repository.ProvinceRepository;

@RestController
@RequestMapping("/api/provinces")
public class ProvinceController {
    private final ProvinceRepository repository;
    public ProvinceController(ProvinceRepository repository) { this.repository = repository; }
    @GetMapping public ApiResponse<List<Province>> findAll() { return ApiResponse.success(repository.findAll()); }
    @GetMapping("/{id}") public ApiResponse<Province> findById(@PathVariable Long id) { return ApiResponse.success(repository.findById(id).orElseThrow()); }
    @GetMapping("/internal/{id}") public ApiResponse<Province> findInternalById(@PathVariable Long id) { return findById(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ApiResponse<Province> create(@RequestBody Province province) { return ApiResponse.success(repository.save(province)); }
    @PutMapping("/{id}") public ApiResponse<Province> update(@PathVariable Long id, @RequestBody Province input) {
        Province current = repository.findById(id).orElseThrow(); current.setCode(input.getCode()); current.setName(input.getName()); current.setType(input.getType()); return ApiResponse.success(repository.save(current));
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id) { repository.deleteById(id); }
}
