package vn.gov.tax.fine.controller;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import vn.gov.tax.common.messaging.KafkaTopics;
import vn.gov.tax.common.messaging.event.FineCreatedEvent;
import vn.gov.tax.common.response.ApiResponse;
import vn.gov.tax.fine.entity.FineDecision;
import vn.gov.tax.fine.entity.FineStatus;
import vn.gov.tax.fine.repository.FineDecisionRepository;
@RestController @RequestMapping("/api/fines") public class FineController {
 private final FineDecisionRepository repository; private final KafkaTemplate<String, FineCreatedEvent> kafka;
 public FineController(FineDecisionRepository repository, KafkaTemplate<String, FineCreatedEvent> kafka){this.repository=repository;this.kafka=kafka;}
 @GetMapping public ApiResponse<List<FineDecision>> findAll(){return ApiResponse.success(repository.findAll());}
 @GetMapping("/{id}") public ApiResponse<FineDecision> findById(@PathVariable Long id){return ApiResponse.success(repository.findById(id).orElseThrow());}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public ApiResponse<FineDecision> create(@RequestBody FineDecision value){FineDecision saved=repository.save(value);kafka.send(KafkaTopics.FINE_CREATED, saved.getDecisionNumber(), new FineCreatedEvent(saved.getId(), saved.getDecisionNumber(), saved.getTaxpayerCode(), saved.getAmount()));return ApiResponse.success(saved);}
 @PatchMapping("/{id}/status") public ApiResponse<FineDecision> updateStatus(@PathVariable Long id,@RequestParam FineStatus status){FineDecision v=repository.findById(id).orElseThrow();v.setStatus(status);return ApiResponse.success(repository.save(v));}
}
