package vn.gov.tax.common.messaging.event;

import java.math.BigDecimal;

public record FineCreatedEvent(Long fineId, String decisionNumber, String taxpayerCode, BigDecimal amount) { }