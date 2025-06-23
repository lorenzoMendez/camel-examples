package com.ibm.camel.routes;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.stereotype.Component;

import com.ibm.camel.entity.AuditLogEntity;
import com.ibm.camel.model.AuditLogEventDto;
import com.ibm.camel.repository.AuditLogRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AuditLoggingRouter extends RouteBuilder {

  /** Repository used to handle the entities. */
  private AuditLogRepository auditLogRepository;

  /**
   * Inject component via constructor.
   *
   * @param auditLogRepository entity component.
   */
  public AuditLoggingRouter(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  /** Router configuration. */
  @Override
  public void configure() throws Exception {
    from("activemq:audit-logging-channel")
        .log("Event received: ${body}")
        .unmarshal()
        .json(JsonLibrary.Jackson, AuditLogEventDto.class)
        .log("Data transformed: ${body}")
        .process(
            exchange -> {
              var auditLogg = exchange.getIn().getBody(AuditLogEventDto.class);
              log.info("auditLogg: {}", auditLogg);
              AuditLogEntity auditLogEntity = new AuditLogEntity();
              auditLogEntity.setDescription(auditLogg.getDescription());
              auditLogEntity.setEvenType(auditLogg.getEvenType());
              auditLogEntity.setStatus(auditLogg.getStatus());
              auditLogEntity = auditLogRepository.save(auditLogEntity);
              log.info("Data persisted with id: {}", auditLogEntity.getId());
            })
        .log("Audit data stored in database.");
  }
}
