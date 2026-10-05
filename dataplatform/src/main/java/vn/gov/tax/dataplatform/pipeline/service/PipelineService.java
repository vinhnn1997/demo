package vn.gov.tax.dataplatform.pipeline.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.gov.tax.dataplatform.compiler.ExecutionPlanCompiler;
import vn.gov.tax.dataplatform.compiler.model.PlanValidationResult;
import vn.gov.tax.dataplatform.pipeline.domain.Pipeline;
import vn.gov.tax.dataplatform.pipeline.dto.PipelineDefinition;
import vn.gov.tax.dataplatform.pipeline.dto.PipelineRequest;
import vn.gov.tax.dataplatform.pipeline.dto.PipelineResponse;
import vn.gov.tax.dataplatform.pipeline.repository.PipelineRepository;
import vn.gov.tax.dataplatform.source.domain.Source;
import vn.gov.tax.dataplatform.source.repository.SourceRepository;
import vn.gov.tax.dataplatform.source.connector.DatabaseConnectionTester;
import vn.gov.tax.dataplatform.source.service.SourceService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PipelineService {
  private final PipelineRepository pipelineRepository;
  private final SourceRepository sourceRepository;
  private final SourceService sourceService;
  private final ObjectMapper objectMapper;
  private final ExecutionPlanCompiler compiler;

  public Page<PipelineResponse> list(String tenantId, Pageable pageable) {
    return pipelineRepository.findAllByTenantIdOrderByNameAsc(tenantId, pageable).map(this::toResponse);
  }

  public PipelineResponse get(String tenantId, UUID id) {
    return toResponse(findPipeline(tenantId, id));
  }

  @Transactional
  public PipelineResponse create(String tenantId, PipelineRequest request) {
    requireActiveSource(tenantId, request.sourceId());
    if (pipelineRepository.existsByTenantIdAndName(tenantId, request.name())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Pipeline name already exists");
    }
    Pipeline pipeline = new Pipeline();
    pipeline.setTenantId(tenantId);
    pipeline.setName(request.name());
    pipeline.setSourceId(request.sourceId());
    pipeline.setDefinitionJson(writeDefinition(request.definition()));
    return toResponse(pipelineRepository.save(pipeline));
  }

  @Transactional
  public PipelineResponse update(String tenantId, UUID id, PipelineRequest request) {
    Pipeline pipeline = findPipeline(tenantId, id);
    requireActiveSource(tenantId, request.sourceId());
    if (pipelineRepository.existsByTenantIdAndNameAndIdNot(tenantId, request.name(), id)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Pipeline name already exists");
    }
    pipeline.setName(request.name());
    pipeline.setSourceId(request.sourceId());
    pipeline.setDefinitionJson(writeDefinition(request.definition()));
    return toResponse(pipelineRepository.save(pipeline));
  }

  public PlanValidationResult compile(String tenantId, UUID id) {
    Pipeline pipeline = findPipeline(tenantId, id);
    Source source = requireActiveSource(tenantId, pipeline.getSourceId());
    try {
      return compiler.compile(
          pipeline.getId(), pipeline.getSourceId(),
          source.getType(),
          objectMapper.readValue(pipeline.getDefinitionJson(), PipelineDefinition.class),
          sourceColumns(tenantId, pipeline));
    } catch (JsonProcessingException exception) {
      return new PlanValidationResult(false, List.of("Saved pipeline definition is invalid"), null);
    } catch (IllegalStateException | IllegalArgumentException exception) {
      return new PlanValidationResult(false, List.of("Source schema could not be inspected"), null);
    }
  }

  public Pipeline findPipeline(String tenantId, UUID id) {
    return pipelineRepository.findByTenantIdAndId(tenantId, id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pipeline not found"));
  }

  public vn.gov.tax.dataplatform.source.dto.DatabaseConnectionRequest sourceConnectionForWorker(
      String tenantId, UUID sourceId) {
    requireActiveSource(tenantId, sourceId);
    return sourceService.connectionForWorker(tenantId, sourceId);
  }

  private Source requireActiveSource(String tenantId, UUID sourceId) {
    return sourceRepository.findByTenantIdAndId(tenantId, sourceId)
        .filter(source -> source.getStatus() == Source.Status.ACTIVE)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active source not found"));
  }

  private String writeDefinition(PipelineDefinition definition) {
    try {
      return objectMapper.writeValueAsString(definition);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Pipeline definition cannot be serialized", exception);
    }
  }

  private Set<String> sourceColumns(String tenantId, Pipeline pipeline) throws JsonProcessingException {
    PipelineDefinition definition = objectMapper.readValue(pipeline.getDefinitionJson(), PipelineDefinition.class);
    return sourceService.discoverColumns(tenantId, pipeline.getSourceId(), definition.sourceTable()).stream()
        .map(DatabaseConnectionTester.DiscoveredColumn::name)
        .collect(java.util.stream.Collectors.toUnmodifiableSet());
  }

  private PipelineResponse toResponse(Pipeline pipeline) {
    try {
      PipelineDefinition definition = objectMapper.readValue(pipeline.getDefinitionJson(), PipelineDefinition.class);
      return new PipelineResponse(
          pipeline.getId(), pipeline.getName(), pipeline.getSourceId(), definition,
          pipeline.getCreatedAt(), pipeline.getUpdatedAt());
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Saved pipeline definition is invalid", exception);
    }
  }
}