package vn.gov.tax.payment.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;
import vn.gov.tax.common.response.ApiResponse;
import vn.gov.tax.payment.entity.Payment;
import vn.gov.tax.payment.entity.PaymentStatus;
import vn.gov.tax.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentRepository repository;


    @GetMapping
    public ApiResponse<List<Payment>> findAll() {
        return ApiResponse.success(repository.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<Payment> findById(@PathVariable Long id) {
        return ApiResponse.success(repository.findById(id).orElseThrow());
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Payment> updateStatus(@PathVariable Long id, @RequestParam PaymentStatus status) {
        Payment v = repository.findById(id).orElseThrow();
        v.setStatus(status);
        return ApiResponse.success(repository.save(v));
    }
}
